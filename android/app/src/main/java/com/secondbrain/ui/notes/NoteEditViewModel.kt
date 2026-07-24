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
    val tags: List<String> = emptyList(),
    val body: String = "",
    val links: List<String> = emptyList(),
    val showLinkPicker: Boolean = false,
    val linkPickerLoading: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed interface NoteEditEvent {
    data class UpdateTitle(val title: String) : NoteEditEvent
    data class SetTags(val tags: List<String>) : NoteEditEvent
    data class UpdateBody(val body: String) : NoteEditEvent
    data object ShowLinkPicker : NoteEditEvent
    data object DismissLinkPicker : NoteEditEvent
    data class SetLinks(val links: List<String>) : NoteEditEvent
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
                            tags = note.tags,
                            body = note.body,
                            links = note.links,
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
            is NoteEditEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is NoteEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            is NoteEditEvent.ShowLinkPicker -> _state.update { it.copy(showLinkPicker = true) }
            is NoteEditEvent.DismissLinkPicker -> _state.update { it.copy(showLinkPicker = false) }
            is NoteEditEvent.SetLinks -> _state.update { it.copy(links = event.links, showLinkPicker = false) }
            is NoteEditEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            val result = if (noteId != null) {
                noteRepository.update(noteId, UpdateNoteRequest(
                    title = s.title,
                    tags = s.tags,
                    body = s.body,
                    links = if (s.links.isNotEmpty()) s.links else null
                ))
            } else {
                noteRepository.create(CreateNoteRequest(
                    title = s.title,
                    tags = s.tags,
                    body = s.body,
                    links = s.links
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
