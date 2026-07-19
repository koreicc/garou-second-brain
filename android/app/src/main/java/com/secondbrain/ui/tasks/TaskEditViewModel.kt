package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateTaskRequest
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditUiState(
    val title: String = "",
    val icon: String = "",
    val location: String = "",
    val tagsInput: String = "",
    val body: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed interface TaskEditEvent {
    data class UpdateTitle(val title: String) : TaskEditEvent
    data class UpdateIcon(val icon: String) : TaskEditEvent
    data class UpdateLocation(val location: String) : TaskEditEvent
    data class UpdateTags(val tags: String) : TaskEditEvent
    data class UpdateBody(val body: String) : TaskEditEvent
    data object Save : TaskEditEvent
}

class TaskEditViewModel(
    private val taskRepository: TaskRepository,
    private val taskId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditUiState())
    val state: StateFlow<TaskEditUiState> = _state.asStateFlow()

    init {
        if (taskId != null) {
            loadTask()
        }
    }

    private fun loadTask() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            taskRepository.get(taskId!!)
                .onSuccess { task ->
                    _state.update {
                        it.copy(
                            title = task.title,
                            icon = task.icon,
                            location = task.location,
                            tagsInput = task.tags.joinToString(", "),
                            body = task.body,
                            isLoading = false
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun onEvent(event: TaskEditEvent) {
        when (event) {
            is TaskEditEvent.UpdateTitle -> _state.update { it.copy(title = event.title) }
            is TaskEditEvent.UpdateIcon -> _state.update { it.copy(icon = event.icon) }
            is TaskEditEvent.UpdateLocation -> _state.update { it.copy(location = event.location) }
            is TaskEditEvent.UpdateTags -> _state.update { it.copy(tagsInput = event.tags) }
            is TaskEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            is TaskEditEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tags = s.tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val result = if (taskId != null) {
                taskRepository.update(taskId, UpdateTaskRequest(
                    title = s.title,
                    icon = s.icon,
                    location = s.location,
                    tags = tags,
                    body = s.body
                ))
            } else {
                taskRepository.create(CreateTaskRequest(
                    title = s.title,
                    icon = s.icon,
                    location = s.location,
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
