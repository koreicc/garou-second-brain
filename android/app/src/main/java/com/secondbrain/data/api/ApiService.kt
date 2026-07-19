package com.secondbrain.data.api

import com.secondbrain.data.dto.*

interface ApiService {
    // Notes
    suspend fun getAllNotes(): List<NoteDto>
    suspend fun getNote(id: String): NoteDto
    suspend fun createNote(request: CreateNoteRequest): NoteDto
    suspend fun updateNote(id: String, request: UpdateNoteRequest): NoteDto
    suspend fun deleteNote(id: String)

    // Tasks
    suspend fun getAllTasks(): List<TaskDto>
    suspend fun getTask(id: String): TaskDto
    suspend fun createTask(request: CreateTaskRequest): TaskDto
    suspend fun updateTask(id: String, request: UpdateTaskRequest): TaskDto
    suspend fun deleteTask(id: String)

    // Quick Tasks
    suspend fun getAllQuickTasks(): List<QuickTaskDto>
    suspend fun createQuickTask(request: CreateQuickTaskRequest): QuickTaskDto
    suspend fun completeQuickTask(id: String): QuickTaskDto
    suspend fun deleteQuickTask(id: String)

    // People
    suspend fun getAllPeople(): List<PersonDto>
    suspend fun getPerson(id: String): PersonDto
    suspend fun createPerson(request: CreatePersonRequest): PersonDto
    suspend fun updatePerson(id: String, request: UpdatePersonRequest): PersonDto
    suspend fun deletePerson(id: String)

    // Search
    suspend fun search(query: String): List<SearchResultDto>
}
