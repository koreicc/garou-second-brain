package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreateQuickTaskRequest
import com.secondbrain.data.dto.toDomain
import com.secondbrain.domain.model.QuickTask

class QuickTaskRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<QuickTask>> = runCatching {
        val response = api.getAllQuickTasks()
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun create(title: String): Result<QuickTask> = runCatching {
        val response = api.createQuickTask(CreateQuickTaskRequest(title = title))
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun complete(id: String): Result<QuickTask> = runCatching {
        val response = api.completeQuickTask(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val response = api.deleteQuickTask(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
    }
}
