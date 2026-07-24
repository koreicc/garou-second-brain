package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.QuickTask

@Serializable
data class QuickTaskDto(
    val id: String,
    val type: String = "quick-task",
    val title: String,
    val status: String = "pending",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class CreateQuickTaskRequest(
    val title: String
)

fun QuickTaskDto.toDomain(): QuickTask = QuickTask(
    id = id,
    type = type,
    title = title,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt
)
