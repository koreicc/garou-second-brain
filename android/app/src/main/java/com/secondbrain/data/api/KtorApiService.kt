package com.secondbrain.data.api

import com.secondbrain.data.dto.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class KtorApiService(
    private val client: HttpClient,
    private val baseUrl: String
) : ApiService {

    private fun tzOffsetMinutes(): String {
        val offset = java.util.TimeZone.getDefault().rawOffset / 60000
        return offset.toString()
    }

    private fun HttpRequestBuilder.withTimezone() {
        header("X-Timezone-Offset", tzOffsetMinutes())
    }

    // ===== Notes =====

    override suspend fun getAllNotes(): ApiResponse<List<NoteDto>> {
        return client.get("$baseUrl/notes").body()
    }

    override suspend fun getNote(id: String): ApiResponse<NoteDto> {
        return client.get("$baseUrl/notes/$id").body()
    }

    override suspend fun createNote(request: CreateNoteRequest): ApiResponse<NoteDto> {
        return client.post("$baseUrl/notes") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updateNote(id: String, request: UpdateNoteRequest): ApiResponse<NoteDto> {
        return client.put("$baseUrl/notes/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun deleteNote(id: String): ApiResponse<Unit> {
        return client.delete("$baseUrl/notes/$id").body()
    }

    // ===== Tasks =====

    override suspend fun getAllTasks(
        status: String?,
        priority: String?,
        search: String?,
        sortBy: String?,
        sortOrder: String?
    ): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks") {
            withTimezone()
            status?.let { parameter("status", it) }
            priority?.let { parameter("priority", it) }
            search?.let { parameter("search", it) }
            sortBy?.let { parameter("sort_by", it) }
            sortOrder?.let { parameter("sort_order", it) }
        }.body()
    }

    override suspend fun getTask(id: String): ApiResponse<TaskDto> {
        return client.get("$baseUrl/tasks/$id") {
            withTimezone()
        }.body()
    }

    override suspend fun getTasksByDate(date: String): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks/by-date") {
            withTimezone()
            parameter("date", date)
        }.body()
    }

    override suspend fun getUpcomingTasks(days: Int): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks/upcoming") {
            withTimezone()
            parameter("days", days.toString())
        }.body()
    }

    override suspend fun createTask(request: CreateTaskRequest): ApiResponse<TaskDto> {
        return client.post("$baseUrl/tasks") {
            withTimezone()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updateTask(id: String, request: UpdateTaskRequest): ApiResponse<TaskDto> {
        return client.put("$baseUrl/tasks/$id") {
            withTimezone()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun deleteTask(id: String): ApiResponse<Unit> {
        return client.delete("$baseUrl/tasks/$id") {
            withTimezone()
        }.body()
    }

    override suspend fun batchTasks(request: BatchTaskRequest): ApiResponse<BatchTaskResponse> {
        return client.post("$baseUrl/tasks/batch") {
            withTimezone()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // ===== Quick Tasks =====

    override suspend fun getAllQuickTasks(): ApiResponse<List<QuickTaskDto>> {
        return client.get("$baseUrl/quick-tasks").body()
    }

    override suspend fun createQuickTask(request: CreateQuickTaskRequest): ApiResponse<QuickTaskDto> {
        return client.post("$baseUrl/quick-tasks") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun completeQuickTask(id: String): ApiResponse<QuickTaskDto> {
        return client.put("$baseUrl/quick-tasks/$id/complete") {
            contentType(ContentType.Application.Json)
        }.body()
    }

    override suspend fun deleteQuickTask(id: String): ApiResponse<Unit> {
        return client.delete("$baseUrl/quick-tasks/$id").body()
    }

    // ===== People =====

    override suspend fun getAllPeople(): ApiResponse<List<PersonDto>> {
        return client.get("$baseUrl/people").body()
    }

    override suspend fun getPerson(id: String): ApiResponse<PersonDto> {
        return client.get("$baseUrl/people/$id").body()
    }

    override suspend fun createPerson(request: CreatePersonRequest): ApiResponse<PersonDto> {
        return client.post("$baseUrl/people") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updatePerson(id: String, request: UpdatePersonRequest): ApiResponse<PersonDto> {
        return client.put("$baseUrl/people/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun deletePerson(id: String): ApiResponse<Unit> {
        return client.delete("$baseUrl/people/$id").body()
    }

    // ===== Search =====

    override suspend fun search(query: String): ApiResponse<List<SearchResultDto>> {
        return client.get("$baseUrl/search") {
            parameter("q", query)
        }.body()
    }

    // ===== WikiLink =====

    override suspend fun resolveWikilink(query: String): ApiResponse<WikilinkResponse> {
        return client.get("$baseUrl/wikilink") {
            parameter("q", query)
        }.body()
    }

    // ===== Entities (batch) =====

    override suspend fun getEntitiesByIds(ids: List<String>): ApiResponse<List<EntityInfoDto>> {
        return client.get("$baseUrl/entities/by-ids") {
            parameter("ids", ids.joinToString(","))
        }.body()
    }

    // ===== Occurrence override =====

    override suspend fun updateOccurrence(parentId: String, date: String, request: UpdateOccurrenceRequest): ApiResponse<TaskDto> {
        return client.put("$baseUrl/tasks/occurrence/$parentId/$date") {
            withTimezone()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    // ===== Habits =====

    override suspend fun getAllHabits(): ApiResponse<List<HabitDto>> {
        return client.get("$baseUrl/habits").body()
    }

    override suspend fun getHabit(id: String): ApiResponse<HabitDto> {
        return client.get("$baseUrl/habits/$id").body()
    }

    override suspend fun createHabit(request: CreateHabitRequest): ApiResponse<HabitDto> {
        return client.post("$baseUrl/habits") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun updateHabit(id: String, request: UpdateHabitRequest): ApiResponse<HabitDto> {
        return client.put("$baseUrl/habits/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun deleteHabit(id: String): ApiResponse<Unit> {
        return client.delete("$baseUrl/habits/$id").body()
    }

    override suspend fun completeHabit(id: String): ApiResponse<HabitDto> {
        return client.post("$baseUrl/habits/$id/complete").body()
    }

    override suspend fun getTodayHabits(): ApiResponse<List<HabitDto>> {
        return client.get("$baseUrl/habits/today") {
            withTimezone()
        }.body()
    }

    // ===== A7 convenience endpoints =====

    override suspend fun getTodayTasks(): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks/today") {
            withTimezone()
        }.body()
    }

    override suspend fun getOverdueTasks(): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks/overdue") {
            withTimezone()
        }.body()
    }

    override suspend fun getAnytimeTasks(): ApiResponse<List<TaskDto>> {
        return client.get("$baseUrl/tasks/anytime") {
            withTimezone()
        }.body()
    }

    override suspend fun completeOccurrence(parentId: String, date: String): ApiResponse<TaskDto> {
        return client.post("$baseUrl/tasks/occurrence/$parentId/$date/complete") {
            withTimezone()
        }.body()
    }

    override suspend fun skipOccurrence(parentId: String, date: String): ApiResponse<TaskDto> {
        return client.post("$baseUrl/tasks/occurrence/$parentId/$date/skip") {
            withTimezone()
        }.body()
    }
}