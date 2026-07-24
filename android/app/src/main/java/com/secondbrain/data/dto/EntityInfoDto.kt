package com.secondbrain.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class EntityInfoDto(
    val id: String,
    val type: String,
    val title: String,
    val status: String
)
