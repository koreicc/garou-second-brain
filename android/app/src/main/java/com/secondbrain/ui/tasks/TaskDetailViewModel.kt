package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface TaskDetailEvent {
    data object Complete : TaskDetailEvent
}

class TaskDetailViewModel(
    private val taskRepository: TaskRepository,
    private val taskId: String
) : ViewModel() {

    private val _state = MutableStateFlow(TaskDetailUiState())
    val state: StateFlow<TaskDetailUiState> = _state.asStateFlow()

    init {
        loadTask()
    }

    /**
     * Reloads the task data without showing a loading indicator.
     * Called when the screen resumes to reflect edits made elsewhere.
     */
    fun reload() {
        viewModelScope.launch {
            taskRepository.get(taskId)
                .onSuccess { task -> _state.update { it.copy(task = task, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun onEvent(event: TaskDetailEvent) {
        when (event) {
            is TaskDetailEvent.Complete -> completeTask()
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            taskRepository.get(taskId)
                .onSuccess { task -> _state.update { it.copy(task = task, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun completeTask() {
        viewModelScope.launch {
            taskRepository.update(taskId, UpdateTaskRequest(status = "completed"))
                .onSuccess { loadTask() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
