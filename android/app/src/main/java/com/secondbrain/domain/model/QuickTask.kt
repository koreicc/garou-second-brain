package com.secondbrain.domain.model

data class QuickTask(
    val id: String,
    val type: String = "quick-task",
    val title: String,
    val status: String = "pending",
    val createdAt: String = "",
    val updatedAt: String = ""
)
