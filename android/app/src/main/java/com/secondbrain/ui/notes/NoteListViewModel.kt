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
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val pendingDeleteNote: Note? = null,
    val isRefreshing: Boolean = false
)

sealed interface NoteListEvent {
    data object LoadNotes : NoteListEvent
    data class DeleteNote(val id: String) : NoteListEvent
    data class ShowDeleteConfirmation(val note: Note) : NoteListEvent
    data object DismissDelete : NoteListEvent
    data object ConfirmDelete : NoteListEvent
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
            is NoteListEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(showDeleteDialog = true, pendingDeleteNote = event.note) }
            }
            is NoteListEvent.DismissDelete -> {
                _state.update { it.copy(showDeleteDialog = false, pendingDeleteNote = null) }
            }
            is NoteListEvent.ConfirmDelete -> {
                val note = _state.value.pendingDeleteNote ?: return
                _state.update { it.copy(showDeleteDialog = false) }
                deleteNote(note.id)
            }
            is NoteListEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    /**
     * Silently reloads without showing the loading indicator.
     * Used by RefreshOnResume to avoid screen flicker.
     */
    fun silentReload() {
        viewModelScope.launch {
            noteRepository.getAll()
                .onSuccess { notes ->
                    _state.update { it.copy(notes = notes, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
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

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            noteRepository.getAll()
                .onSuccess { notes -> _state.update { it.copy(notes = notes, isRefreshing = false) } }
                .onFailure { e -> _state.update { it.copy(isRefreshing = false, error = e.message) } }
        }
    }

    private fun deleteNote(id: String) {
        viewModelScope.launch {
            // Optimistic removal
            _state.update { it.copy(notes = it.notes.filter { n -> n.id != id }) }

            noteRepository.delete(id)
                .onSuccess { silentReload() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    silentReload()
                }
        }
    }
}
