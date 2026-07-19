package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.*
import com.secondbrain.domain.model.Note

class NoteRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Note>> = runCatching {
        api.getAllNotes().map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<Note> = runCatching {
        api.getNote(id).toDomain()
    }

    suspend fun create(
        title: String,
        tags: List<String> = emptyList(),
        content: String = ""
    ): Result<Note> = runCatching {
        api.createNote(CreateNoteRequest(title = title, tags = tags, content = content)).toDomain()
    }

    suspend fun update(
        id: String,
        title: String? = null,
        status: String? = null,
        tags: List<String>? = null,
        content: String? = null
    ): Result<Note> = runCatching {
        api.updateNote(id, UpdateNoteRequest(title = title, status = status, tags = tags, content = content)).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deleteNote(id)
    }
}
