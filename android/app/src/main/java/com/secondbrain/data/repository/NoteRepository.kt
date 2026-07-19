package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreateNoteRequest
import com.secondbrain.data.dto.UpdateNoteRequest
import com.secondbrain.domain.model.Note

class NoteRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Note>> = runCatching {
        val response = api.getAllNotes()
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun get(id: String): Result<Note> = runCatching {
        val response = api.getNote(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Note not found")).toDomain()
    }

    suspend fun create(request: CreateNoteRequest): Result<Note> = runCatching {
        val response = api.createNote(request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun update(id: String, request: UpdateNoteRequest): Result<Note> = runCatching {
        val response = api.updateNote(id, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val response = api.deleteNote(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
    }
}
