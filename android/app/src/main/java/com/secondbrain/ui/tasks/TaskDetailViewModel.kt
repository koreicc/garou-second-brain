package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.repository.SearchRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Task
import com.secondbrain.data.dto.toDto
import com.secondbrain.ui.notes.WikilinkNavigationTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val wikilinkNavigationTarget: WikilinkNavigationTarget? = null
)

sealed interface TaskDetailEvent {
    data object Complete : TaskDetailEvent
    data class ToggleSubtask(val subtaskId: String) : TaskDetailEvent
    data class ResolveWikilink(val target: String) : TaskDetailEvent
}

class TaskDetailViewModel(
    private val taskRepository: TaskRepository,
    private val searchRepository: SearchRepository,
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
            is TaskDetailEvent.ToggleSubtask -> toggleSubtask(event.subtaskId)
            is TaskDetailEvent.ResolveWikilink -> resolveWikilink(event.target)
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

    private fun toggleSubtask(subtaskId: String) {
        viewModelScope.launch {
            val task = _state.value.task ?: return@launch
            val updatedSubtasks = task.subtasks.map { subtask ->
                if (subtask.id == subtaskId) subtask.copy(completed = !subtask.completed) else subtask
            }
            taskRepository.update(
                taskId,
                UpdateTaskRequest(subtasks = updatedSubtasks.map { it.toDto() })
            )
                .onSuccess { loadTask() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun resolveWikilink(target: String) {
        viewModelScope.launch {
            searchRepository.search(target)
                .onSuccess { results ->
                    when {
                        results.isEmpty() -> {
                            _state.update { it.copy(error = "No entity found for [[$target]]") }
                        }
                        results.size == 1 -> {
                            val result = results.first()
                            _state.update {
                                it.copy(wikilinkNavigationTarget = WikilinkNavigationTarget(
                                    type = result.type,
                                    id = result.id
                                ))
                            }
                        }
                        else -> {
                            _state.update { it.copy(error = "Multiple entities match [[$target]]; be more specific") }
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = "Failed to resolve [[$target]]: ${e.message}") }
                }
        }
    }

    /**
     * Called after navigation has been triggered to reset the navigation state.
     */
    fun clearWikilinkNavigation() {
        _state.update { it.copy(wikilinkNavigationTarget = null) }
    }
}
