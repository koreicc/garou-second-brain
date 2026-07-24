package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreateTaskRequest
import com.secondbrain.data.dto.UpdateOccurrenceRequest
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.dto.toDomain
import com.secondbrain.domain.model.Task

class TaskRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Task>> = runCatching {
        val response = api.getAllTasks()
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

    suspend fun updateOccurrence(parentId: String, date: String, request: UpdateOccurrenceRequest): Result<Task> = runCatching {
        val response = api.updateOccurrence(parentId, date, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }
}
