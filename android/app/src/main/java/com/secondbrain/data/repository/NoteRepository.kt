package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.ApiResponse
import com.secondbrain.data.dto.CreateNoteRequest
import com.secondbrain.data.dto.UpdateNoteRequest
import com.secondbrain.domain.model.Note

class NoteRepository(private val api: ApiService) {

    private fun <T> checkError(response: ApiResponse<T>): T {
        if (response.error.isNotBlank()) {
            throw Exception(response.error)
        }
        return response.data ?: throw Exception("Empty response")
    }

    suspend fun getAll(): Result<List<Note>> = runCatching {
        checkError(api.getAllNotes()).map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<Note> = runCatching {
        checkError(api.getNote(id)).toDomain()
    }

    suspend fun create(
        title: String,
        tags: List<String> = emptyList(),
        content: String = ""
    ): Result<Note> = runCatching {
        checkError(api.createNote(CreateNoteRequest(title = title, tags = tags, content = content))).toDomain()
    }

    suspend fun update(
        id: String,
        title: String? = null,
        status: String? = null,
        tags: List<String>? = null,
        content: String? = null
    ): Result<Note> = runCatching {
        checkError(api.updateNote(id, UpdateNoteRequest(title = title, status = status, tags = tags, content = content))).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        checkError(api.deleteNote(id))
    }
}
