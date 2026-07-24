package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.*

@Serializable
data class TaskDto(
    val id: String,
    val type: String = "task",
    val title: String,
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("parent_id")
    val parentId: String = "",
    @SerialName("is_template")
    val isTemplate: Boolean = false,
    @SerialName("occurrence_date")
    val occurrenceDate: String = "",
    // Date fields
    @SerialName("date_mode")
    val dateMode: String = "",
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    // Time fields
    @SerialName("time_mode")
    val timeMode: String = "",
    @SerialName("start_time")
    val startTime: String = "",
    @SerialName("end_time")
    val endTime: String = "",
    @SerialName("duration_minutes")
    val durationMinutes: Int = 0,
    @SerialName("due_time")
    val dueTime: String = "",
    // Recurrence & subtasks
    val recurrence: RecurrenceDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
    val body: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class RecurrenceDto(
    val type: String,
    val interval: Int = 1,
    @SerialName("days_of_week")
    val daysOfWeek: List<Int> = emptyList()
)

@Serializable
data class SubtaskDto(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("parent_id")
    val parentId: String = "",
    @SerialName("is_template")
    val isTemplate: Boolean = false,
    @SerialName("occurrence_date")
    val occurrenceDate: String = "",
    // Date fields
    @SerialName("date_mode")
    val dateMode: String = "",
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    // Time fields
    @SerialName("time_mode")
    val timeMode: String = "",
    @SerialName("start_time")
    val startTime: String = "",
    @SerialName("end_time")
    val endTime: String = "",
    @SerialName("duration_minutes")
    val durationMinutes: Int = 0,
    @SerialName("due_time")
    val dueTime: String = "",
    // Recurrence & subtasks
    val recurrence: RecurrenceDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
    val body: String = ""
)

@Serializable
data class UpdateTaskRequest(
    val title: String? = null,
    val status: String? = null,
    val icon: String? = null,
    val location: String? = null,
    val tags: List<String>? = null,
    val links: List<String>? = null,
    @SerialName("parent_id")
    val parentId: String? = null,
    @SerialName("is_template")
    val isTemplate: Boolean? = null,
    @SerialName("occurrence_date")
    val occurrenceDate: String? = null,
    @SerialName("propagate_to_occurrences")
    val propagateToOccurrences: Boolean = false,
    // Date fields
    @SerialName("date_mode")
    val dateMode: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    // Time fields
    @SerialName("time_mode")
    val timeMode: String? = null,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    @SerialName("due_time")
    val dueTime: String? = null,
    // Recurrence & subtasks
    val recurrence: RecurrenceDto? = null,
    val subtasks: List<SubtaskDto>? = null,
    val body: String? = null
)

fun TaskDto.toDomain(): Task = Task(
    id = id,
    type = type,
    title = title,
    status = status,
    icon = icon,
    location = location,
    tags = tags,
    links = links,
    parentId = parentId,
    isTemplate = isTemplate,
    occurrenceDate = occurrenceDate,
    dateMode = dateMode,
    dueDate = dueDate ?: "",
    startDate = startDate ?: "",
    endDate = endDate ?: "",
    timeMode = timeMode,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    dueTime = dueTime,
    recurrence = recurrence?.toDomain(),
    subtasks = subtasks.map { it.toDomain() },
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecurrenceDto.toDomain(): Recurrence = Recurrence(
    type = type,
    interval = interval,
    daysOfWeek = daysOfWeek
)

fun SubtaskDto.toDomain(): Subtask = Subtask(
    id = id,
    title = title,
    completed = completed
)

fun Subtask.toDto(): SubtaskDto = SubtaskDto(
    id = id,
    title = title,
    completed = completed
)

fun Recurrence.toDto(): RecurrenceDto = RecurrenceDto(
    type = type,
    interval = interval,
    daysOfWeek = daysOfWeek
)

@Serializable
data class UpdateOccurrenceRequest(
    val status: String? = null,
    val title: String? = null,
    val body: String? = null,
    val subtasks: List<SubtaskDto>? = null
)
