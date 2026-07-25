package com.secondbrain.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateHabitRequest
import com.secondbrain.data.dto.SubtaskDto
import com.secondbrain.data.dto.UpdateHabitRequest
import com.secondbrain.data.repository.HabitRepository
import com.secondbrain.domain.model.Subtask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class HabitTimePickerType { Due, Start, End }

data class HabitEditUiState(
    // Core
    val title: String = "",
    val icon: String = "",
    val location: String = "",
    val priority: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val body: String = "",
    // Days of week (1=Mon..7=Sun)
    val daysOfWeek: List<Int> = emptyList(),
    // Time
    val timeMode: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: String = "",
    val dueTime: String = "",
    // Subtasks
    val subtasks: List<HabitSubtaskItem> = emptyList(),
    val newSubtaskTitle: String = "",
    // UI state
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val showIconPicker: Boolean = false,
    val showLinkPicker: Boolean = false,
    val activeTimePicker: HabitTimePickerType? = null
)

data class HabitSubtaskItem(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

sealed interface HabitEditEvent {
    // Core
    data class UpdateTitle(val title: String) : HabitEditEvent
    data class UpdateIcon(val icon: String) : HabitEditEvent
    data class UpdateLocation(val location: String) : HabitEditEvent
    data class UpdatePriority(val priority: String) : HabitEditEvent
    data class UpdateBody(val body: String) : HabitEditEvent
    data class SetTags(val tags: List<String>) : HabitEditEvent
    data class SetLinks(val links: List<String>) : HabitEditEvent
    // Days
    data class ToggleDay(val day: Int) : HabitEditEvent
    // Time
    data class SetTimeMode(val mode: String) : HabitEditEvent
    data class SetTime(val type: HabitTimePickerType, val time: String) : HabitEditEvent
    data class SetDuration(val minutes: String) : HabitEditEvent
    // Subtasks
    data class UpdateNewSubtaskTitle(val title: String) : HabitEditEvent
    data object AddSubtask : HabitEditEvent
    data class RemoveSubtask(val id: String) : HabitEditEvent
    data class ToggleSubtask(val id: String) : HabitEditEvent
    data class ReorderSubtasks(val fromIndex: Int, val toIndex: Int) : HabitEditEvent
    // UI
    data object ShowIconPicker : HabitEditEvent
    data object DismissIconPicker : HabitEditEvent
    data object ShowLinkPicker : HabitEditEvent
    data object DismissLinkPicker : HabitEditEvent
    data class ShowTimePicker(val type: HabitTimePickerType) : HabitEditEvent
    data object DismissTimePicker : HabitEditEvent
    // Action
    data object Save : HabitEditEvent
    data object DismissError : HabitEditEvent
}

class HabitEditViewModel(
    private val habitRepository: HabitRepository,
    private val habitId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(HabitEditUiState())
    val state: StateFlow<HabitEditUiState> = _state.asStateFlow()

    init {
        if (habitId != null) {
            loadHabit()
        }
    }

    private fun loadHabit() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val id = habitId ?: return@launch
            habitRepository.get(id)
                .onSuccess { habit ->
                    _state.update {
                        it.copy(
                            title = habit.title,
                            icon = habit.icon,
                            location = habit.location,
                            priority = habit.priority,
                            tags = habit.tags,
                            links = habit.links,
                            body = habit.body,
                            daysOfWeek = habit.daysOfWeek,
                            timeMode = habit.timeMode,
                            startTime = habit.startTime,
                            endTime = habit.endTime,
                            durationMinutes = if (habit.durationMinutes > 0) habit.durationMinutes.toString() else "",
                            dueTime = habit.dueTime,
                            subtasks = habit.subtasks.map { s ->
                                HabitSubtaskItem(id = s.id, title = s.title, completed = s.completed)
                            },
                            isLoading = false
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun onEvent(event: HabitEditEvent) {
        when (event) {
            // Core
            is HabitEditEvent.UpdateTitle -> _state.update { it.copy(title = event.title, error = null) }
            is HabitEditEvent.UpdateIcon -> _state.update { it.copy(icon = event.icon, showIconPicker = false) }
            is HabitEditEvent.UpdateLocation -> _state.update { it.copy(location = event.location) }
            is HabitEditEvent.UpdatePriority -> _state.update { it.copy(priority = event.priority) }
            is HabitEditEvent.UpdateBody -> _state.update { it.copy(body = event.body) }
            is HabitEditEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is HabitEditEvent.SetLinks -> _state.update { it.copy(links = event.links, showLinkPicker = false) }
            // Days
            is HabitEditEvent.ToggleDay -> _state.update {
                val newDays = if (event.day in it.daysOfWeek) {
                    it.daysOfWeek - event.day
                } else {
                    it.daysOfWeek + event.day
                }
                it.copy(daysOfWeek = newDays.sorted())
            }
            // Time
            is HabitEditEvent.SetTimeMode -> _state.update { it.copy(timeMode = event.mode) }
            is HabitEditEvent.SetTime -> _state.update {
                when (event.type) {
                    HabitTimePickerType.Due -> it.copy(dueTime = event.time, activeTimePicker = null)
                    HabitTimePickerType.Start -> it.copy(startTime = event.time, activeTimePicker = null)
                    HabitTimePickerType.End -> it.copy(endTime = event.time, activeTimePicker = null)
                }
            }
            is HabitEditEvent.SetDuration -> _state.update { it.copy(durationMinutes = event.minutes) }
            // Subtasks
            is HabitEditEvent.UpdateNewSubtaskTitle -> _state.update { it.copy(newSubtaskTitle = event.title) }
            is HabitEditEvent.AddSubtask -> addSubtask()
            is HabitEditEvent.RemoveSubtask -> _state.update {
                it.copy(subtasks = it.subtasks.filter { s -> s.id != event.id })
            }
            is HabitEditEvent.ToggleSubtask -> _state.update {
                it.copy(subtasks = it.subtasks.map { s ->
                    if (s.id == event.id) s.copy(completed = !s.completed) else s
                })
            }
            is HabitEditEvent.ReorderSubtasks -> _state.update { state ->
                val list = state.subtasks.toMutableList()
                if (event.fromIndex < 0 || event.fromIndex >= list.size ||
                    event.toIndex < 0 || event.toIndex >= list.size) return@update state
                val item = list.removeAt(event.fromIndex)
                list.add(event.toIndex, item)
                state.copy(subtasks = list)
            }
            // UI
            is HabitEditEvent.ShowIconPicker -> _state.update { it.copy(showIconPicker = true) }
            is HabitEditEvent.DismissIconPicker -> _state.update { it.copy(showIconPicker = false) }
            is HabitEditEvent.ShowLinkPicker -> _state.update { it.copy(showLinkPicker = true) }
            is HabitEditEvent.DismissLinkPicker -> _state.update { it.copy(showLinkPicker = false) }
            is HabitEditEvent.ShowTimePicker -> _state.update { it.copy(activeTimePicker = event.type) }
            is HabitEditEvent.DismissTimePicker -> _state.update { it.copy(activeTimePicker = null) }
            // Action
            is HabitEditEvent.Save -> save()
            is HabitEditEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun addSubtask() {
        val title = _state.value.newSubtaskTitle.trim()
        if (title.isEmpty()) return
        _state.update {
            it.copy(
                subtasks = it.subtasks + HabitSubtaskItem(id = UUID.randomUUID().toString(), title = title),
                newSubtaskTitle = ""
            )
        }
    }

    private fun save() {
        val s = _state.value
        if (s.title.isBlank() || s.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            val durationMin = s.durationMinutes.toIntOrNull() ?: 0
            val subtaskDtos = s.subtasks.map { SubtaskDto(id = it.id, title = it.title, completed = it.completed) }

            val result = if (habitId != null) {
                habitRepository.update(habitId, UpdateHabitRequest(
                    title = s.title,
                    icon = s.icon.ifEmpty { null },
                    location = s.location.ifEmpty { null },
                    priority = s.priority.ifEmpty { null },
                    tags = if (s.tags.isNotEmpty()) s.tags else null,
                    links = if (s.links.isNotEmpty()) s.links else null,
                    daysOfWeek = s.daysOfWeek,
                    timeMode = s.timeMode.ifBlank { null },
                    startTime = s.startTime.ifBlank { null },
                    endTime = s.endTime.ifBlank { null },
                    durationMinutes = if (durationMin > 0) durationMin else null,
                    dueTime = s.dueTime.ifBlank { null },
                    subtasks = if (subtaskDtos.isNotEmpty()) subtaskDtos else null,
                    body = s.body.ifEmpty { null }
                ))
            } else {
                habitRepository.create(CreateHabitRequest(
                    title = s.title,
                    icon = s.icon.ifEmpty { null },
                    location = s.location.ifEmpty { null },
                    priority = s.priority.ifEmpty { null },
                    tags = s.tags,
                    links = s.links,
                    daysOfWeek = s.daysOfWeek,
                    timeMode = s.timeMode.ifBlank { null },
                    startTime = s.startTime.ifBlank { null },
                    endTime = s.endTime.ifBlank { null },
                    durationMinutes = if (durationMin > 0) durationMin else null,
                    dueTime = s.dueTime.ifBlank { null },
                    subtasks = subtaskDtos,
                    body = s.body
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isSaving = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isSaving = false, error = e.message) } }
        }
    }
}
