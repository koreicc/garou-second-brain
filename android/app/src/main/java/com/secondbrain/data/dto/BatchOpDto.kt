package com.secondbrain.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BatchTaskRequest(
    val ids: List<String>,
    val action: String // "delete" or "complete"
)

@Serializable
data class BatchTaskResponse(
    val success: Int = 0,
    val errors: List<String> = emptyList()
)