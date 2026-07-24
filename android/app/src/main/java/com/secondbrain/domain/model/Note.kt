package com.secondbrain.domain.model

data class Note(
    val id: String,
    val type: String = "note",
    val title: String,
    val status: String = "active",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val body: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
