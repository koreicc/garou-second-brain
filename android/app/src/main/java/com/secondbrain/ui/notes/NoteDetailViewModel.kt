package com.secondbrain.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.UpdateNoteRequest
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteDetailUiState(
    val note: Note? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface NoteDetailEvent {
    data object ToggleArchive : NoteDetailEvent
}

class NoteDetailViewModel(
    private val noteRepository: NoteRepository,
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
}
