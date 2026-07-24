package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateTaskRequest
import com.secondbrain.data.dto.RecurrenceDto
import com.secondbrain.data.dto.SubtaskDto
import com.secondbrain.data.dto.UpdateOccurrenceRequest
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Subtask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class DatePickerType { Due, Start, End }
enum class TimePickerType { Due, Start, End }

data class TaskEditUiState(
    // Core
    val title: String = "",
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val priority: String = "",
    val tags: List<String> = emptyList(),
    val body: String = "",
    val links: List<String> = emptyList(),
    // Template / occurrence
    val isTemplate: Boolean = false,
    val parentId: String = "",
    val occurrenceDate: String = "",
    val isOccurrenceEdit: Boolean = false,
    val showEditModeDialog: Boolean = false,
    // Date (only meaningful for templates / new tasks)
    val dateMode: String = "",
    val dueDate: String = "",
    val startDate: String = "",
    val endDate: String = "",
    // Time
    val timeMode: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: String = "",
    val dueTime: String = "",
    // Recurrence (only for templates)
    val recurrenceType: String? = null,
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: List<Int> = emptyList(),
    // Subtasks
    val subtasks: List<SubtaskEditItem> = emptyList(),
    val newSubtaskTitle: String = "",
    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val showLinkPicker: Boolean = false,
    val showIconPicker: Boolean = false,
    val activeDatePicker: DatePickerType? = null,
    val activeTimePicker: TimePickerType? = null,
)

data class SubtaskEditItem(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

sealed interface TaskEditEvent {
    // Core
    data class UpdateTitle(val title: String) : TaskEditEvent
    data class UpdateStatus(val status: String) : TaskEditEvent
    data class UpdateIcon(val icon: String) : TaskEditEvent
    data class UpdateLocation(val location: String) : TaskEditEvent
    data class UpdatePriority(val priority: String) : TaskEditEvent
    data class UpdateBody(val body: String) : TaskEditEvent
    data class SetTags(val tags: List<String>) : TaskEditEvent
    data class SetLinks(val links: List<String>) : TaskEditEvent
    // Date / Time
    data class SetDateMode(val mode: String) : TaskEditEvent
    data class SetDate(val type: DatePickerType, val date: String) : TaskEditEvent
    data class SetTimeMode(val mode: String) : TaskEditEvent
    data class SetTime(val type: TimePickerType, val time: String) : TaskEditEvent
    data class SetDuration(val minutes: String) : TaskEditEvent
    // Recurrence (template only)
    data class SetRecurrenceType(val type: String?) : TaskEditEvent
    data class SetRecurrenceInterval(val interval: Int) : TaskEditEvent
    data class SetRecurrenceDays(val days: List<Int>) : TaskEditEvent
    // Occurrence
    data class SetOccurrenceEdit(val editThisOnly: Boolean) : TaskEditEvent
    data object DismissEditModeDialog : TaskEditEvent
    // Subtasks
    data class UpdateNewSubtaskTitle(val title: String) : TaskEditEvent
    data object AddSubtask : TaskEditEvent
    data class RemoveSubtask(val id: String) : TaskEditEvent
    data class ToggleSubtask(val id: String) : TaskEditEvent
    data class MoveSubtask(val id: String, val direction: Int) : TaskEditEvent
    // UI
    data object ShowLinkPicker : TaskEditEvent
    data object DismissLinkPicker : TaskEditEvent
    data object ShowIconPicker : TaskEditEvent
    data object DismissIconPicker : TaskEditEvent
    data class ShowDatePicker(val type: DatePickerType) : TaskEditEvent
    data object DismissDatePicker : TaskEditEvent
    data class ShowTimePicker(val type: TimePickerType) : TaskEditEvent
    data object DismissTimePicker : TaskEditEvent
    // Action
    data object Save : TaskEditEvent
    data object DismissError : TaskEditEvent
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
            val id = taskId ?: return@launch
            taskRepository.get(id)
                .onSuccess { task ->
                    _state.update {
                        it.copy(
                            title = task.title,
                            status = task.status,
                            icon = task.icon,
                            location = task.location,
                            priority = task.priority,
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
                            parentId = task.parentId,
                            isTemplate = task.isTemplate,
                            occurrenceDate = task.occurrenceDate,
                            isLoading = false,
                            showEditModeDialog = task.parentId.isNotEmpty()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onEvent(event: TaskEditEvent) {
        when (event) {
            // Core
            is TaskEditEvent.UpdateTitle -> _state.update { it.copy(title = event.title, error = null) }
            is TaskEditEvent.UpdateStatus -> _state.update { it.copy(status = event.status) }
            is TaskEditEvent.UpdateIcon -> _state.update { it.copy(icon = event.icon, showIconPicker = false) }
            is TaskEditEvent.UpdateLocation -> _state.update { it.copy(location = event.location) }
            is TaskEditEvent.UpdatePriority -> _state.update { it.copy(priority = event.priority) }
            is TaskEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            is TaskEditEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is TaskEditEvent.SetLinks -> _state.update { it.copy(links = event.links, showLinkPicker = false) }
            // Date / Time
            is TaskEditEvent.SetDateMode -> _state.update {
                it.copy(
                    dateMode = event.mode,
                    // Preserve recurrence when switching back to range
                    recurrenceType = if (event.mode != "range") null else it.recurrenceType
                )
            }
            is TaskEditEvent.SetDate -> _state.update {
                when (event.type) {
                    DatePickerType.Due -> it.copy(dueDate = event.date, activeDatePicker = null)
                    DatePickerType.Start -> it.copy(startDate = event.date, activeDatePicker = null)
                    DatePickerType.End -> it.copy(endDate = event.date, activeDatePicker = null)
                }
            }
            is TaskEditEvent.SetTimeMode -> _state.update { it.copy(timeMode = event.mode) }
            is TaskEditEvent.SetTime -> _state.update {
                when (event.type) {
                    TimePickerType.Due -> it.copy(dueTime = event.time, activeTimePicker = null)
                    TimePickerType.Start -> it.copy(startTime = event.time, activeTimePicker = null)
                    TimePickerType.End -> it.copy(endTime = event.time, activeTimePicker = null)
                }
            }
            is TaskEditEvent.SetDuration -> _state.update { it.copy(durationMinutes = event.minutes) }
            // Recurrence
            is TaskEditEvent.SetRecurrenceType -> _state.update {
                it.copy(
                    recurrenceType = event.type,
                    // Only clear days when switching away from weekly
                    recurrenceDaysOfWeek = if (event.type == "weekly") it.recurrenceDaysOfWeek else emptyList()
                )
            }
            is TaskEditEvent.SetRecurrenceInterval -> _state.update { it.copy(recurrenceInterval = event.interval) }
            is TaskEditEvent.SetRecurrenceDays -> _state.update { it.copy(recurrenceDaysOfWeek = event.days) }
            // Occurrence
            is TaskEditEvent.SetOccurrenceEdit -> {
                if (!event.editThisOnly) {
                    // User wants to edit the template -- reload with template data
                    val parentId = _state.value.parentId
                    if (parentId.isNotEmpty()) {
                        _state.update { it.copy(isOccurrenceEdit = false, showEditModeDialog = false, isLoading = true) }
                        viewModelScope.launch {
                            taskRepository.get(parentId)
                                .onSuccess { task ->
                                    _state.update {
                                        it.copy(
                                            title = task.title,
                                            status = task.status,
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
                                            parentId = "",
                                            isTemplate = task.isTemplate,
                                            occurrenceDate = "",
                                            isLoading = false
                                        )
                                    }
                                }
                                .onFailure { e ->
                                    _state.update { it.copy(isLoading = false, error = e.message) }
                                }
                        }
                    }
                } else {
                    _state.update { it.copy(isOccurrenceEdit = true, showEditModeDialog = false) }
                }
            }
            is TaskEditEvent.DismissEditModeDialog -> _state.update { it.copy(showEditModeDialog = false) }
            // Subtasks
            is TaskEditEvent.UpdateNewSubtaskTitle -> _state.update { it.copy(newSubtaskTitle = event.title) }
            is TaskEditEvent.AddSubtask -> addSubtask()
            is TaskEditEvent.RemoveSubtask -> _state.update {
                it.copy(subtasks = it.subtasks.filter { s -> s.id != event.id })
            }
            is TaskEditEvent.ToggleSubtask -> _state.update {
                it.copy(subtasks = it.subtasks.map { s ->
                    if (s.id == event.id) s.copy(completed = !s.completed) else s
                })
            }
            is TaskEditEvent.MoveSubtask -> _state.update { state ->
                val list = state.subtasks.toMutableList()
                val index = list.indexOfFirst { it.id == event.id }
                if (index == -1) return@update state
                val newIndex = index + event.direction
                if (newIndex < 0 || newIndex >= list.size) return@update state
                val item = list.removeAt(index)
                list.add(newIndex, item)
                state.copy(subtasks = list)
            }
            // UI
            is TaskEditEvent.ShowLinkPicker -> _state.update { it.copy(showLinkPicker = true) }
            is TaskEditEvent.DismissLinkPicker -> _state.update { it.copy(showLinkPicker = false) }
            is TaskEditEvent.ShowIconPicker -> _state.update { it.copy(showIconPicker = true) }
            is TaskEditEvent.DismissIconPicker -> _state.update { it.copy(showIconPicker = false) }
            is TaskEditEvent.ShowDatePicker -> _state.update { it.copy(activeDatePicker = event.type) }
            is TaskEditEvent.DismissDatePicker -> _state.update { it.copy(activeDatePicker = null) }
            is TaskEditEvent.ShowTimePicker -> _state.update { it.copy(activeTimePicker = event.type) }
            is TaskEditEvent.DismissTimePicker -> _state.update { it.copy(activeTimePicker = null) }
            // Action
            is TaskEditEvent.Save -> save()
            is TaskEditEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun addSubtask() {
        val title = _state.value.newSubtaskTitle.trim()
        if (title.isEmpty()) return
        _state.update {
            it.copy(
                subtasks = it.subtasks + SubtaskEditItem(id = UUID.randomUUID().toString(), title = title),
                newSubtaskTitle = ""
            )
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank() || s.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            val dueDateISO = if (s.dateMode == "due_date" && s.dueDate.isNotBlank()) "${s.dueDate}T00:00:00Z" else null
            val startDateISO = if (s.dateMode == "range" && s.startDate.isNotBlank()) "${s.startDate}T00:00:00Z" else null
            val endDateISO = if (s.dateMode == "range" && s.endDate.isNotBlank()) "${s.endDate}T23:59:59Z" else null

            val recurrence = if (s.dateMode == "range" && s.isTemplate) {
                s.recurrenceType?.let { type ->
                    RecurrenceDto(
                        type = type,
                        interval = s.recurrenceInterval,
                        daysOfWeek = if (type == "weekly") s.recurrenceDaysOfWeek else emptyList()
                    )
                }
            } else null

            val durationMin = s.durationMinutes.toIntOrNull() ?: 0
            val subtaskDtos = s.subtasks.map { SubtaskDto(id = it.id, title = it.title, completed = it.completed) }

            val result = if (taskId != null) {
                if (s.isOccurrenceEdit && s.parentId.isNotEmpty()) {
                    // Update single occurrence via override endpoint
                    taskRepository.updateOccurrence(
                        s.parentId,
                        s.occurrenceDate,
                        UpdateOccurrenceRequest(
                            status = s.status,
                            title = s.title,
                            body = s.body,
                            subtasks = subtaskDtos
                        )
                    )
                } else {
                    // Update template or standalone task
                    taskRepository.update(taskId, UpdateTaskRequest(
                        title = s.title,
                        status = s.status,
                        icon = s.icon,
                        location = s.location,
                        priority = s.priority,
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
                }
            } else {
                taskRepository.create(CreateTaskRequest(
                    title = s.title,
                    status = s.status,
                    icon = s.icon,
                    location = s.location,
                    priority = s.priority,
                    isTemplate = s.dateMode == "range" && s.recurrenceType != null,
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
                .onSuccess { _state.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
