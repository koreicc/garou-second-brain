package com.secondbrain.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.QuickTaskRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val noteCount: Int = 0,
    val taskCount: Int = 0,
    val personCount: Int = 0,
    val recentNotes: List<Note> = emptyList(),
    val recentTasks: List<Task> = emptyList(),
    val quickTasks: List<QuickTask> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val quickTaskInput: String = ""
)

sealed interface DashboardEvent {
    data class LoadData(val force: Boolean = false) : DashboardEvent
    data class UpdateQuickTaskInput(val input: String) : DashboardEvent
    data object CreateQuickTask : DashboardEvent
    data class CompleteQuickTask(val id: String) : DashboardEvent
    data class DismissError(val message: String) : DashboardEvent
}

class DashboardViewModel(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val quickTaskRepository: QuickTaskRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LoadData -> loadData(force = event.force)
            is DashboardEvent.UpdateQuickTaskInput -> {
                _state.update { it.copy(quickTaskInput = event.input) }
            }
            is DashboardEvent.CreateQuickTask -> createQuickTask()
            is DashboardEvent.CompleteQuickTask -> completeQuickTask(event.id)
            is DashboardEvent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadData(force: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load counts and quick tasks in parallel
            launch {
                noteRepository.getAll().onSuccess { notes ->
                    _state.update { it.copy(noteCount = notes.size, recentNotes = notes.take(5)) }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }

            launch {
                taskRepository.getAll().onSuccess { tasks ->
                    _state.update { it.copy(taskCount = tasks.size, recentTasks = tasks.take(5)) }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }

            launch {
                personRepository.getAll().onSuccess { people ->
                    _state.update { it.copy(personCount = people.size) }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }

            launch {
                quickTaskRepository.getAll().onSuccess { quickTasks ->
                    _state.update { it.copy(quickTasks = quickTasks) }
                }.onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
            }

            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun createQuickTask() {
        val title = _state.value.quickTaskInput.trim()
        if (title.isEmpty()) return

        viewModelScope.launch {
            quickTaskRepository.create(title).onSuccess {
                _state.update { state ->
                    state.copy(quickTaskInput = "")
                }
                loadData()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun completeQuickTask(id: String) {
        viewModelScope.launch {
            quickTaskRepository.complete(id).onSuccess {
                loadData()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }
}
