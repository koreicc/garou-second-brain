package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.BatchTaskRequest
import com.secondbrain.data.dto.CreateTaskRequest
import com.secondbrain.data.dto.UpdateOccurrenceRequest
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.dto.toDomain
import com.secondbrain.domain.model.Task

class TaskRepository(private val api: ApiService) {

    suspend fun getAll(
        status: String? = null,
        priority: String? = null,
        search: String? = null,
        sortBy: String? = null,
        sortOrder: String? = null
    ): Result<List<Task>> = runCatching {
        val response = api.getAllTasks(status, priority, search, sortBy, sortOrder)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun get(id: String): Result<Task> = runCatching {
        val response = api.getTask(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Task not found")).toDomain()
    }

    suspend fun getByDate(date: String): Result<List<Task>> = runCatching {
        val response = api.getTasksByDate(date)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun getUpcoming(days: Int = 7): Result<List<Task>> = runCatching {
        val response = api.getUpcomingTasks(days)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun create(request: CreateTaskRequest): Result<Task> = runCatching {
        val response = api.createTask(request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun update(id: String, request: UpdateTaskRequest): Result<Task> = runCatching {
        val response = api.updateTask(id, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val response = api.deleteTask(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
    }

    suspend fun batch(ids: List<String>, action: String): Result<Unit> = runCatching {
        val response = api.batchTasks(BatchTaskRequest(ids = ids, action = action))
        if (response.error.isNotBlank()) throw Exception(response.error)
        val result = response.data
        if (result != null && result.errors.isNotEmpty()) {
            throw Exception("Batch errors: ${result.errors.joinToString("; ")}")
        }
    }

    suspend fun updateOccurrence(parentId: String, date: String, request: UpdateOccurrenceRequest): Result<Task> = runCatching {
        val response = api.updateOccurrence(parentId, date, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }
}