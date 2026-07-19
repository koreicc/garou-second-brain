package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.QuickTask

@Serializable
data class QuickTaskDto(
    val id: String,
    val title: String,
    val status: String = "pending",
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class CreateQuickTaskRequest(
    val title: String
)

@Serializable
data class UpdateQuickTaskRequest(
    val status: String
)

fun QuickTaskDto.toDomain(): QuickTask = QuickTask(
    id = id,
    title = title,
    status = status,
    createdAt = createdAt
)
