package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.Note

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val status: String = "active",
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val body: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class CreateNoteRequest(
    val title: String,
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val body: String = ""
)

@Serializable
data class UpdateNoteRequest(
    val title: String? = null,
    val status: String? = null,
    val tags: List<String>? = null,
    val links: List<String>? = null,
    val body: String? = null
)

fun NoteDto.toDomain(): Note = Note(
    id = id,
    title = title,
    status = status,
    tags = tags,
    links = links,
    body = body,
    createdAt = createdAt,
    updatedAt = updatedAt
)
