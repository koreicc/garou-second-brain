package com.secondbrain.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.UpdateNoteRequest
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.SearchRepository
import com.secondbrain.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WikilinkNavigationTarget(
    val type: String,
    val id: String
)

data class NoteDetailUiState(
    val note: Note? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val wikilinkNavigationTarget: WikilinkNavigationTarget? = null
)

sealed interface NoteDetailEvent {
    data object ToggleArchive : NoteDetailEvent
    data class ResolveWikilink(val target: String) : NoteDetailEvent
}

class NoteDetailViewModel(
    private val noteRepository: NoteRepository,
    private val searchRepository: SearchRepository,
    private val noteId: String
) : ViewModel() {

    private val _state = MutableStateFlow(NoteDetailUiState())
    val state: StateFlow<NoteDetailUiState> = _state.asStateFlow()

    init {
        loadNote()
    }

    fun onEvent(event: NoteDetailEvent) {
        when (event) {
            is NoteDetailEvent.ToggleArchive -> toggleArchive()
            is NoteDetailEvent.ResolveWikilink -> resolveWikilink(event.target)
        }
    }

    /**
     * Reloads the note data without showing a loading indicator.
     * Called when the screen resumes to reflect edits made elsewhere.
     */
    fun reload() {
        viewModelScope.launch {
            noteRepository.get(noteId)
                .onSuccess { note ->
                    _state.update { it.copy(note = note, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            noteRepository.get(noteId)
                .onSuccess { note ->
                    _state.update { it.copy(note = note, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun toggleArchive() {
        val currentNote = _state.value.note ?: return
        val newStatus = if (currentNote.status == "archived") "active" else "archived"
        viewModelScope.launch {
            noteRepository.update(noteId, UpdateNoteRequest(status = newStatus))
                .onSuccess { updatedNote ->
                    _state.update { it.copy(note = updatedNote) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun resolveWikilink(target: String) {
        viewModelScope.launch {
            searchRepository.search(target)
                .onSuccess { results ->
                    when {
                        results.isEmpty() -> {
                            _state.update { it.copy(error = "No entity found for [[$target]]") }
                        }
                        results.size == 1 -> {
                            val result = results.first()
                            _state.update {
                                it.copy(wikilinkNavigationTarget = WikilinkNavigationTarget(
                                    type = result.type,
                                    id = result.id
                                ))
                            }
                        }
                        else -> {
                            _state.update { it.copy(error = "Multiple entities match [[$target]]; be more specific") }
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to resolve [[$target]]: ${e.message}") }
                }
        }
    }

    /**
     * Called after navigation has been triggered to reset the navigation state.
     */
    fun clearWikilinkNavigation() {
        _state.update { it.copy(wikilinkNavigationTarget = null) }
    }
}
