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
    val error: String? = null
)

sealed interface TaskListEvent {
    data object LoadTasks : TaskListEvent
    data class DeleteTask(val id: String) : TaskListEvent
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
            taskRepository.delete(id)
                .onSuccess { loadTasks() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
