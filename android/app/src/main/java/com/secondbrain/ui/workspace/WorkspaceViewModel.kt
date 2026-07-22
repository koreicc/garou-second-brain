package com.secondbrain.ui.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.Person
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkspaceUiState(
    val selectedTab: Int = 0,
    val notes: List<Note> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val people: List<Person> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Delete confirmation
    val showDeleteDialog: Boolean = false,
    val pendingDeleteNote: Note? = null,
    val pendingDeleteTask: Task? = null,
    val pendingDeletePerson: Person? = null
)

sealed interface WorkspaceEvent {
    data class SelectTab(val index: Int) : WorkspaceEvent
    data class UpdateSearchQuery(val query: String) : WorkspaceEvent
    data object LoadData : WorkspaceEvent
    // Entity delete
    data class ShowDeleteNote(val note: Note) : WorkspaceEvent
    data class ShowDeleteTask(val task: Task) : WorkspaceEvent
    data class ShowDeletePerson(val person: Person) : WorkspaceEvent
    data object ConfirmDelete : WorkspaceEvent
    data object DismissDelete : WorkspaceEvent
    data object DismissError : WorkspaceEvent
}

class WorkspaceViewModel(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkspaceUiState())
    val state: StateFlow<WorkspaceUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: WorkspaceEvent) {
        when (event) {
            is WorkspaceEvent.SelectTab -> _state.update { it.copy(selectedTab = event.index) }
            is WorkspaceEvent.UpdateSearchQuery -> _state.update { it.copy(searchQuery = event.query) }
            is WorkspaceEvent.LoadData -> loadData()
            is WorkspaceEvent.ShowDeleteNote -> _state.update {
                it.copy(showDeleteDialog = true, pendingDeleteNote = event.note)
            }
            is WorkspaceEvent.ShowDeleteTask -> _state.update {
                it.copy(showDeleteDialog = true, pendingDeleteTask = event.task)
            }
            is WorkspaceEvent.ShowDeletePerson -> _state.update {
                it.copy(showDeleteDialog = true, pendingDeletePerson = event.person)
            }
            is WorkspaceEvent.ConfirmDelete -> confirmDelete()
            is WorkspaceEvent.DismissDelete -> _state.update {
                it.copy(
                    showDeleteDialog = false,
                    pendingDeleteNote = null,
                    pendingDeleteTask = null,
                    pendingDeletePerson = null
                )
            }
            is WorkspaceEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    /**
     * Silently reloads all data without showing the loading indicator.
     * Used by RefreshOnResume to avoid screen flicker.
     */
    fun silentReload() {
        viewModelScope.launch {
            val result = coroutineScope {
                val notesDeferred = async { noteRepository.getAll().getOrDefault(emptyList()) }
                val tasksDeferred = async { taskRepository.getAll().getOrDefault(emptyList()) }
                val peopleDeferred = async { personRepository.getAll().getOrDefault(emptyList()) }
                Triple(notesDeferred.await(), tasksDeferred.await(), peopleDeferred.await())
            }
            _state.update {
                it.copy(
                    notes = result.first,
                    tasks = result.second,
                    people = result.third
                )
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                coroutineScope {
                    val notesDeferred = async { noteRepository.getAll().getOrDefault(emptyList()) }
                    val tasksDeferred = async { taskRepository.getAll().getOrDefault(emptyList()) }
                    val peopleDeferred = async { personRepository.getAll().getOrDefault(emptyList()) }
                    _state.update {
                        it.copy(
                            notes = notesDeferred.await(),
                            tasks = tasksDeferred.await(),
                            people = peopleDeferred.await(),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun confirmDelete() {
        val currentState = _state.value
        viewModelScope.launch {
            currentState.pendingDeleteNote?.let { note ->
                noteRepository.delete(note.id)
                    .onSuccess { silentReload() }
                    .onFailure { error -> _state.update { it.copy(error = error.message) } }
            }
            currentState.pendingDeleteTask?.let { task ->
                taskRepository.delete(task.id)
                    .onSuccess { silentReload() }
                    .onFailure { error -> _state.update { it.copy(error = error.message) } }
            }
            currentState.pendingDeletePerson?.let { person ->
                personRepository.delete(person.id)
                    .onSuccess { silentReload() }
                    .onFailure { error -> _state.update { it.copy(error = error.message) } }
            }
            _state.update {
                it.copy(
                    showDeleteDialog = false,
                    pendingDeleteNote = null,
                    pendingDeleteTask = null,
                    pendingDeletePerson = null
                )
            }
        }
    }
}

/**
 * Returns [Result.getOrNull] ?: [default] to avoid wrapping every call-site in runCatching.
 */
private fun <T> Result<T>.getOrDefault(default: T): T = getOrNull() ?: default
