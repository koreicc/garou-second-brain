package com.secondbrain.domain.model

data class QuickTask(
    val id: String,
    val title: String,
    val status: String = "pending",
    val createdAt: String = ""
)
