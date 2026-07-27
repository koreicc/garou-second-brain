package com.secondbrain.domain.model

data class Habit(
    val id: String,
    val type: String = "habit",
    val status: String = "active",
    val title: String,
    val icon: String = "",
    val location: String = "",
    val priority: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val daysOfWeek: List<Int> = emptyList(),
    val timeMode: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val durationMinutes: Int = 0,
    val dueTime: String = "",
    val subtasks: List<Subtask> = emptyList(),
    val body: String = "",
    val todayCompleted: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)
