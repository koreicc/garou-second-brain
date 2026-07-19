package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.ApiResponse
import com.secondbrain.data.dto.CreateQuickTaskRequest
import com.secondbrain.domain.model.QuickTask

class QuickTaskRepository(private val api: ApiService) {

    private fun <T> checkError(response: ApiResponse<T>): T {
        if (response.error.isNotBlank()) {
            throw Exception(response.error)
        }
        return response.data ?: throw Exception("Empty response")
    }

    suspend fun getAll(): Result<List<QuickTask>> = runCatching {
        checkError(api.getAllQuickTasks()).map { it.toDomain() }
    }

    suspend fun create(title: String): Result<QuickTask> = runCatching {
        checkError(api.createQuickTask(CreateQuickTaskRequest(title = title))).toDomain()
    }

    suspend fun complete(id: String): Result<QuickTask> = runCatching {
        checkError(api.completeQuickTask(id)).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        checkError(api.deleteQuickTask(id))
    }
}
