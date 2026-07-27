package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.*

@Serializable
data class HabitDto(
    val id: String,
    val type: String = "habit",
    val status: String = "active",
    val title: String,
    val icon: String? = null,
    val location: String? = null,
    val priority: String? = null,
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("days_of_week")
    val daysOfWeek: List<Int> = emptyList(),
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
    val subtasks: List<SubtaskDto> = emptyList(),
    val body: String = "",
    @SerialName("today_completed")
    val todayCompleted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class CreateHabitRequest(
    val title: String,
    val icon: String? = null,
    val location: String? = null,
    val priority: String? = null,
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    @SerialName("days_of_week")
    val daysOfWeek: List<Int>,
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
    val subtasks: List<SubtaskDto> = emptyList(),
    val body: String = ""
)

@Serializable
data class UpdateHabitRequest(
    val title: String? = null,
    val icon: String? = null,
    val location: String? = null,
    val priority: String? = null,
    val tags: List<String>? = null,
    val links: List<String>? = null,
    @SerialName("days_of_week")
    val daysOfWeek: List<Int>? = null,
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
    val subtasks: List<SubtaskDto>? = null,
    val body: String? = null
)

fun HabitDto.toDomain(): Habit = Habit(
    id = id,
    type = type,
    status = status,
    title = title,
    icon = icon ?: "",
    location = location ?: "",
    priority = priority ?: "",
    tags = tags,
    links = links,
    daysOfWeek = daysOfWeek,
    timeMode = timeMode ?: "",
    startTime = startTime ?: "",
    endTime = endTime ?: "",
    durationMinutes = durationMinutes ?: 0,
    dueTime = dueTime ?: "",
    subtasks = subtasks.map { it.toDomain() },
    body = body,
    todayCompleted = todayCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Habit.toDto(): HabitDto = HabitDto(
    id = id,
    type = type,
    status = status,
    title = title,
    icon = icon.ifEmpty { null },
    location = location.ifEmpty { null },
    priority = priority.ifEmpty { null },
    tags = tags,
    links = links,
    daysOfWeek = daysOfWeek,
    timeMode = timeMode.ifEmpty { null },
    startTime = startTime.ifEmpty { null },
    endTime = endTime.ifEmpty { null },
    durationMinutes = durationMinutes.takeIf { it > 0 },
    dueTime = dueTime.ifEmpty { null },
    subtasks = subtasks.map { it.toDto() },
    body = body,
    todayCompleted = todayCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)
