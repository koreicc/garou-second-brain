package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val pendingDeleteTask: Task? = null
)

sealed interface TaskListEvent {
    data object LoadTasks : TaskListEvent
    data class DeleteTask(val id: String) : TaskListEvent
    data class ShowDeleteConfirmation(val task: Task) : TaskListEvent
    data object DismissDelete : TaskListEvent
    data object ConfirmDelete : TaskListEvent
}

class TaskListViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListUiState())
    val state: StateFlow<TaskListUiState> = _state.asStateFlow()

    init {
        loadTasks()
    }

    fun onEvent(event: TaskListEvent) {
        when (event) {
            is TaskListEvent.LoadTasks -> loadTasks()
            is TaskListEvent.DeleteTask -> deleteTask(event.id)
            is TaskListEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(showDeleteDialog = true, pendingDeleteTask = event.task) }
            }
            is TaskListEvent.DismissDelete -> {
                _state.update { it.copy(showDeleteDialog = false, pendingDeleteTask = null) }
            }
            is TaskListEvent.ConfirmDelete -> {
                val task = _state.value.pendingDeleteTask ?: return
                _state.update { it.copy(showDeleteDialog = false) }
                deleteTask(task.id)
            }
        }
    }

    /**
     * Silently reloads without showing the loading indicator.
     */
    fun silentReload() {
        viewModelScope.launch {
            taskRepository.getAll()
                .onSuccess { tasks ->
                    _state.update { it.copy(tasks = tasks) }
                }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            taskRepository.getAll()
                .onSuccess { tasks -> _state.update { it.copy(tasks = tasks, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun deleteTask(id: String) {
        viewModelScope.launch {
            // Optimistic removal
            _state.update { it.copy(tasks = it.tasks.filter { t -> t.id != id }) }

            taskRepository.delete(id)
                .onSuccess { silentReload() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    silentReload()
                }
        }
    }
}
