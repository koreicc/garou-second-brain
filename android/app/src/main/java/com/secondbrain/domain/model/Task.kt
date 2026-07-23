package com.secondbrain.domain.model

data class Task(
    val id: String,
    val title: String,
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    // Date fields
    val dateMode: String = "",           // "due_date" | "range" | ""
    val dueDate: String = "",            // ISO date for due_date mode
    val startDate: String = "",           // ISO date for range mode
    val endDate: String = "",             // ISO date for range mode
    // Time fields
    val timeMode: String = "",            // "due_time" | "start_end" | "start_duration" | ""
    val startTime: String = "",           // "HH:mm"
    val endTime: String = "",             // "HH:mm"
    val durationMinutes: Int = 0,          // minutes
    val dueTime: String = "",             // "HH:mm"
    // Recurrence & subtasks
    val recurrence: Recurrence? = null,
    val subtasks: List<Subtask> = emptyList(),
    val body: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class Subtask(
    val id: String,
    val title: String,
    val completed: Boolean = false
)

data class Recurrence(
    val type: String,  // daily, weekly, monthly, yearly
    val interval: Int = 1,
    val daysOfWeek: List<Int> = emptyList()
)
