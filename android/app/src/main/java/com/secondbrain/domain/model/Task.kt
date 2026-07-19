package com.secondbrain.domain.model

data class Task(
    val id: String,
    val title: String,
    val status: String = "pending",
    val icon: String = "",
    val location: String = "",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
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
