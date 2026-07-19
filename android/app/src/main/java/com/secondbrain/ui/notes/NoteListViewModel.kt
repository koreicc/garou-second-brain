package com.secondbrain.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.domain.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface NoteListEvent {
    data object LoadNotes : NoteListEvent
    data class DeleteNote(val id: String) : NoteListEvent
    data class DismissError(val message: String) : NoteListEvent
}

class NoteListViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(NoteListUiState())
    val state: StateFlow<NoteListUiState> = _state.asStateFlow()

    init {
        loadNotes()
    }

    fun onEvent(event: NoteListEvent) {
        when (event) {
            is NoteListEvent.LoadNotes -> loadNotes()
            is NoteListEvent.DeleteNote -> deleteNote(event.id)
            is NoteListEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadNotes() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            noteRepository.getAll()
                .onSuccess { notes ->
                    _state.update { it.copy(notes = notes, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun deleteNote(id: String) {
        viewModelScope.launch {
            noteRepository.delete(id)
                .onSuccess { loadNotes() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }
}
