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
    val links: List<String> = emptyList(),
    val showLinkPicker: Boolean = false,
    val linkPickerLoading: Boolean = false,
    // Icon picker
    val showIconPicker: Boolean = false,
    // Date mode: "", "due_date", "range"
    val dateMode: String = "",
    val dueDate: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val showDueDatePicker: Boolean = false,
    val showStartDatePicker: Boolean = false,
    val showEndDatePicker: Boolean = false,
    // Time mode: "", "due_time", "start_end", "start_duration"
    val timeMode: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: String = "",    // stored as string for text field
    val dueTime: String = "",
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
    // Date mode events
    data class SetDateMode(val mode: String) : TaskEditEvent
    // Due date events
    data object ShowDueDatePicker : TaskEditEvent
    data object DismissDueDatePicker : TaskEditEvent
    data class SetDueDate(val date: String) : TaskEditEvent
    // Range date events
    data object ShowStartDatePicker : TaskEditEvent
    data object DismissStartDatePicker : TaskEditEvent
    data class SetStartDate(val date: String) : TaskEditEvent
    data object ShowEndDatePicker : TaskEditEvent
    data object DismissEndDatePicker : TaskEditEvent
    data class SetEndDate(val date: String) : TaskEditEvent
    // Time mode events
    data class SetTimeMode(val mode: String) : TaskEditEvent
    // Time value events
    data class SetStartTime(val time: String) : TaskEditEvent
    data class SetEndTime(val time: String) : TaskEditEvent
    data class SetDurationMinutes(val minutes: String) : TaskEditEvent
    data class SetDueTime(val time: String) : TaskEditEvent
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
    // Link Picker events
    data object ShowLinkPicker : TaskEditEvent
    data object DismissLinkPicker : TaskEditEvent
    data class SetLinks(val links: List<String>) : TaskEditEvent
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
                            links = task.links,
                            dateMode = task.dateMode,
                            dueDate = task.dueDate.take(10),
                            startDate = task.startDate.take(10),
                            endDate = task.endDate.take(10),
                            timeMode = task.timeMode,
                            startTime = task.startTime,
                            endTime = task.endTime,
                            durationMinutes = if (task.durationMinutes > 0) task.durationMinutes.toString() else "",
                            dueTime = task.dueTime,
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
            // Date mode
            is TaskEditEvent.SetDateMode -> _state.update {
                it.copy(dateMode = event.mode, recurrenceType = if (event.mode != "range") null else it.recurrenceType)
            }
            // Due date
            is TaskEditEvent.ShowDueDatePicker -> _state.update { it.copy(showDueDatePicker = true) }
            is TaskEditEvent.DismissDueDatePicker -> _state.update { it.copy(showDueDatePicker = false) }
            is TaskEditEvent.SetDueDate -> _state.update { it.copy(dueDate = event.date, showDueDatePicker = false) }
            // Range dates
            is TaskEditEvent.ShowStartDatePicker -> _state.update { it.copy(showStartDatePicker = true) }
            is TaskEditEvent.DismissStartDatePicker -> _state.update { it.copy(showStartDatePicker = false) }
            is TaskEditEvent.SetStartDate -> _state.update { it.copy(startDate = event.date, showStartDatePicker = false) }
            is TaskEditEvent.ShowEndDatePicker -> _state.update { it.copy(showEndDatePicker = true) }
            is TaskEditEvent.DismissEndDatePicker -> _state.update { it.copy(showEndDatePicker = false) }
            is TaskEditEvent.SetEndDate -> _state.update { it.copy(endDate = event.date, showEndDatePicker = false) }
            // Time mode
            is TaskEditEvent.SetTimeMode -> _state.update { it.copy(timeMode = event.mode) }
            // Time values
            is TaskEditEvent.SetStartTime -> _state.update { it.copy(startTime = event.time) }
            is TaskEditEvent.SetEndTime -> _state.update { it.copy(endTime = event.time) }
            is TaskEditEvent.SetDurationMinutes -> _state.update { it.copy(durationMinutes = event.minutes) }
            is TaskEditEvent.SetDueTime -> _state.update { it.copy(dueTime = event.time) }
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
            // Link picker
            is TaskEditEvent.ShowLinkPicker -> _state.update { it.copy(showLinkPicker = true) }
            is TaskEditEvent.DismissLinkPicker -> _state.update { it.copy(showLinkPicker = false) }
            is TaskEditEvent.SetLinks -> _state.update { it.copy(links = event.links, showLinkPicker = false) }
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

            // Build date/time ISO strings
            val dueDateISO = if (s.dateMode == "due_date" && s.dueDate.isNotBlank()) "${s.dueDate}T00:00:00Z" else null
            val startDateISO = if (s.dateMode == "range" && s.startDate.isNotBlank()) "${s.startDate}T00:00:00Z" else null
            val endDateISO = if (s.dateMode == "range" && s.endDate.isNotBlank()) "${s.endDate}T23:59:59Z" else null

            val recurrence = if (s.dateMode == "range") {
                s.recurrenceType?.let { type ->
                    RecurrenceDto(
                        type = type,
                        interval = s.recurrenceInterval,
                        daysOfWeek = if (type == "weekly") s.recurrenceDaysOfWeek else emptyList()
                    )
                }
            } else {
                null
            }

            val durationMin = s.durationMinutes.toIntOrNull() ?: 0

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
                    links = if (s.links.isNotEmpty()) s.links else null,
                    dateMode = s.dateMode.ifBlank { null },
                    dueDate = dueDateISO,
                    startDate = startDateISO,
                    endDate = endDateISO,
                    timeMode = s.timeMode.ifBlank { null },
                    startTime = s.startTime.ifBlank { null },
                    endTime = s.endTime.ifBlank { null },
                    durationMinutes = if (durationMin > 0) durationMin else null,
                    dueTime = s.dueTime.ifBlank { null },
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
                    links = s.links,
                    dateMode = s.dateMode,
                    dueDate = dueDateISO,
                    startDate = startDateISO,
                    endDate = endDateISO,
                    timeMode = s.timeMode,
                    startTime = s.startTime,
                    endTime = s.endTime,
                    durationMinutes = durationMin,
                    dueTime = s.dueTime,
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
