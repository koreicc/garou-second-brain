package com.secondbrain.data.api

import com.secondbrain.data.dto.*

interface ApiService {
    // Notes
    suspend fun getAllNotes(): ApiResponse<List<NoteDto>>
    suspend fun getNote(id: String): ApiResponse<NoteDto>
    suspend fun createNote(request: CreateNoteRequest): ApiResponse<NoteDto>
    suspend fun updateNote(id: String, request: UpdateNoteRequest): ApiResponse<NoteDto>
    suspend fun deleteNote(id: String): ApiResponse<Unit>

    // Tasks
    suspend fun getAllTasks(): ApiResponse<List<TaskDto>>
    suspend fun getTask(id: String): ApiResponse<TaskDto>
    suspend fun getTasksByDate(date: String): ApiResponse<List<TaskDto>>
    suspend fun createTask(request: CreateTaskRequest): ApiResponse<TaskDto>
    suspend fun updateTask(id: String, request: UpdateTaskRequest): ApiResponse<TaskDto>
    suspend fun deleteTask(id: String): ApiResponse<Unit>

    // Quick Tasks
    suspend fun getAllQuickTasks(): ApiResponse<List<QuickTaskDto>>
    suspend fun createQuickTask(request: CreateQuickTaskRequest): ApiResponse<QuickTaskDto>
    suspend fun completeQuickTask(id: String): ApiResponse<QuickTaskDto>
    suspend fun deleteQuickTask(id: String): ApiResponse<Unit>

    // People
    suspend fun getAllPeople(): ApiResponse<List<PersonDto>>
    suspend fun getPerson(id: String): ApiResponse<PersonDto>
    suspend fun createPerson(request: CreatePersonRequest): ApiResponse<PersonDto>
    suspend fun updatePerson(id: String, request: UpdatePersonRequest): ApiResponse<PersonDto>
    suspend fun deletePerson(id: String): ApiResponse<Unit>

    // Search
    suspend fun search(query: String): ApiResponse<List<SearchResultDto>>

    // WikiLink
    suspend fun resolveWikilink(query: String): ApiResponse<WikilinkResponse>

    // Entities (batch)
    suspend fun getEntitiesByIds(ids: List<String>): ApiResponse<List<EntityInfoDto>>
}
