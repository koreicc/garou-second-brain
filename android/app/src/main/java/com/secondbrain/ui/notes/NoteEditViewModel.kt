package com.secondbrain.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateNoteRequest
import com.secondbrain.data.dto.UpdateNoteRequest
import com.secondbrain.data.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditUiState(
    val title: String = "",
    val tagsInput: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed interface NoteEditEvent {
    data class UpdateTitle(val title: String) : NoteEditEvent
    data class UpdateTags(val tags: String) : NoteEditEvent
    data class UpdateBody(val body: String) : NoteEditEvent
    data object Save : NoteEditEvent
}

class NoteEditViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(NoteEditUiState())
    val state: StateFlow<NoteEditUiState> = _state.asStateFlow()

    init {
        if (noteId != null) {
            loadNote()
        }
    }

    private fun loadNote() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            noteRepository.get(noteId!!)
                .onSuccess { note ->
                    _state.update {
                        it.copy(
                            title = note.title,
                            tagsInput = note.tags.joinToString(", "),
                            body = note.body,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onEvent(event: NoteEditEvent) {
        when (event) {
            is NoteEditEvent.UpdateTitle -> _state.update { it.copy(title = event.title) }
            is NoteEditEvent.UpdateTags -> _state.update { it.copy(tagsInput = event.tags) }
            is NoteEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            is NoteEditEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tags = s.tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val result = if (noteId != null) {
                noteRepository.update(noteId, UpdateNoteRequest(
                    title = s.title,
                    tags = tags,
                    body = s.body
                ))
            } else {
                noteRepository.create(CreateNoteRequest(
                    title = s.title,
                    tags = tags,
                    body = s.body
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
