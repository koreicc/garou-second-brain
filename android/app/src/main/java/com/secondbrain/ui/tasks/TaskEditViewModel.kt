package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateTaskRequest
import com.secondbrain.data.dto.RecurrenceDto
import com.secondbrain.data.dto.SubtaskDto
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Recurrence
import com.secondbrain.domain.model.Subtask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class TaskEditUiState(
    val title: String = "",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val body: String = "",
    // Icon picker
    val showIconPicker: Boolean = false,
    // Date fields as ISO date strings (YYYY-MM-DD)
    val startDate: String = "",
    val endDate: String = "",
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    // Recurrence
    val recurrenceType: String? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    // Subtasks
    val subtasks: List<SubtaskEditItem> = emptyList(),
    val newSubtaskTitle: String = "",
    // Loading/save
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

data class SubtaskEditItem(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

sealed interface TaskEditEvent {
    data class UpdateTitle(val title: String) : TaskEditEvent
    data class UpdateIcon(val icon: String) : TaskEditEvent
    data class UpdateLocation(val location: String) : TaskEditEvent
    data class SetTags(val tags: List<String>) : TaskEditEvent
    data class UpdateBody(val body: String) : TaskEditEvent
    // Icon events
    data object ShowIconPicker : TaskEditEvent
    data object DismissIconPicker : TaskEditEvent
    // Date events
    data object ShowStartDatePicker : TaskEditEvent
    data object DismissStartDatePicker : TaskEditEvent
    data class SetStartDate(val date: String) : TaskEditEvent
    data object ShowEndDatePicker : TaskEditEvent
    data object DismissEndDatePicker : TaskEditEvent
    data class SetEndDate(val date: String) : TaskEditEvent
    // Recurrence events
    data class SetRecurrenceType(val type: String?) : TaskEditEvent
    data class SetRecurrenceInterval(val interval: Int) : TaskEditEvent
    data class SetRecurrenceDaysOfWeek(val days: List<Int>) : TaskEditEvent
    // Subtask events
    data class UpdateNewSubtaskTitle(val title: String) : TaskEditEvent
    data object AddSubtask : TaskEditEvent
    data class RemoveSubtask(val id: String) : TaskEditEvent
    data class ToggleSubtask(val id: String) : TaskEditEvent
    data class MoveSubtask(val id: String, val direction: Int) : TaskEditEvent
    // Save
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
                            tags = task.tags,
                            body = task.body,
                            startDate = task.startDate.take(10),
                            endDate = task.endDate.take(10),
                            recurrenceType = task.recurrence?.type,
                            recurrenceInterval = task.recurrence?.interval ?: 1,
                            recurrenceDaysOfWeek = task.recurrence?.daysOfWeek ?: emptyList(),
                            subtasks = task.subtasks.map { s ->
                                SubtaskEditItem(id = s.id, title = s.title, completed = s.completed)
                            },
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
            is TaskEditEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is TaskEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            // Icon events
            is TaskEditEvent.ShowIconPicker -> _state.update { it.copy(showIconPicker = true) }
            is TaskEditEvent.DismissIconPicker -> _state.update { it.copy(showIconPicker = false) }
            // Date events
            is TaskEditEvent.ShowStartDatePicker -> _state.update { it.copy(showStartDatePicker = true) }
            is TaskEditEvent.DismissStartDatePicker -> _state.update { it.copy(showStartDatePicker = false) }
            is TaskEditEvent.SetStartDate -> _state.update { it.copy(startDate = event.date, showStartDatePicker = false) }
            is TaskEditEvent.ShowEndDatePicker -> _state.update { it.copy(showEndDatePicker = true) }
            is TaskEditEvent.DismissEndDatePicker -> _state.update { it.copy(showEndDatePicker = false) }
            is TaskEditEvent.SetEndDate -> _state.update { it.copy(endDate = event.date, showEndDatePicker = false) }
            // Recurrence
            is TaskEditEvent.SetRecurrenceType -> _state.update { it.copy(recurrenceType = event.type, recurrenceDaysOfWeek = emptyList()) }
            is TaskEditEvent.SetRecurrenceInterval -> _state.update { it.copy(recurrenceInterval = event.interval) }
            is TaskEditEvent.SetRecurrenceDaysOfWeek -> _state.update { it.copy(recurrenceDaysOfWeek = event.days) }
            // Subtasks
            is TaskEditEvent.UpdateNewSubtaskTitle -> _state.update { it.copy(newSubtaskTitle = event.title) }
            is TaskEditEvent.AddSubtask -> addSubtask()
            is TaskEditEvent.RemoveSubtask -> removeSubtask(event.id)
            is TaskEditEvent.ToggleSubtask -> toggleSubtask(event.id)
            is TaskEditEvent.MoveSubtask -> moveSubtask(event.id, event.direction)
            // Save
            is TaskEditEvent.Save -> save()
        }
    }

    private fun addSubtask() {
        val title = _state.value.newSubtaskTitle.trim()
        if (title.isEmpty()) return
        val newSubtask = SubtaskEditItem(
            id = UUID.randomUUID().toString(),
            title = title
        )
        _state.update {
            it.copy(
                subtasks = it.subtasks + newSubtask,
                newSubtaskTitle = ""
            )
        }
    }

    private fun removeSubtask(id: String) {
        _state.update { it.copy(subtasks = it.subtasks.filter { s -> s.id != id }) }
    }

    private fun toggleSubtask(id: String) {
        _state.update {
            it.copy(
                subtasks = it.subtasks.map { s ->
                    if (s.id == id) s.copy(completed = !s.completed) else s
                }
            )
        }
    }

    private fun moveSubtask(id: String, direction: Int) {
        _state.update { state ->
            val list = state.subtasks.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index == -1) return@update state
            val newIndex = index + direction
            if (newIndex < 0 || newIndex >= list.size) return@update state
            val item = list.removeAt(index)
            list.add(newIndex, item)
            state.copy(subtasks = list)
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val startDateISO = if (s.startDate.isNotBlank()) "${s.startDate}T00:00:00Z" else null
            val endDateISO = if (s.endDate.isNotBlank()) "${s.endDate}T23:59:59Z" else null

            val recurrence = s.recurrenceType?.let { type ->
                RecurrenceDto(
                    type = type,
                    interval = s.recurrenceInterval,
                    daysOfWeek = if (type == "weekly") s.recurrenceDaysOfWeek else emptyList()
                )
            }

            val subtaskDtos = s.subtasks.map { sub ->
                SubtaskDto(id = sub.id, title = sub.title, completed = sub.completed)
            }

            val result = if (taskId != null) {
                taskRepository.update(taskId, UpdateTaskRequest(
                    title = s.title,
                    icon = s.icon,
                    location = s.location,
                    tags = if (s.tags.isNotEmpty()) s.tags else null,
                    body = s.body,
                    startDate = startDateISO,
                    endDate = endDateISO,
                    recurrence = recurrence,
                    subtasks = if (subtaskDtos.isNotEmpty()) subtaskDtos else null
                ))
            } else {
                taskRepository.create(CreateTaskRequest(
                    title = s.title,
                    icon = s.icon,
                    location = s.location,
                    tags = s.tags,
                    body = s.body,
                    startDate = startDateISO,
                    endDate = endDateISO,
                    recurrence = recurrence,
                    subtasks = subtaskDtos
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
