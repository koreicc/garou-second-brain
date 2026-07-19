package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.*

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("start_date")
    val startDate: String = "",
    @SerialName("end_date")
    val endDate: String = "",
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
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("start_date")
    val startDate: String = "",
    @SerialName("end_date")
    val endDate: String = "",
    val recurrence: RecurrenceDto? = null,
    val subtasks: List<SubtaskDto> = emptyList(),
    @SerialName("body")
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
    @SerialName("start_date")
    val startDate: String? = null,
    @SerialName("end_date")
    val endDate: String? = null,
    val recurrence: RecurrenceDto? = null,
    val subtasks: List<SubtaskDto>? = null,
    @SerialName("body")
    val body: String? = null
)

fun TaskDto.toDomain(): Task = Task(
    id = id,
    title = title,
    status = status,
    icon = icon,
    location = location,
    tags = tags,
    links = links,
    startDate = startDate,
    endDate = endDate,
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
