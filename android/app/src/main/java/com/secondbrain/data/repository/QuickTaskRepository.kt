package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreateQuickTaskRequest
import com.secondbrain.domain.model.QuickTask

class QuickTaskRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<QuickTask>> = runCatching {
        api.getAllQuickTasks().map { it.toDomain() }
    }

    suspend fun create(title: String): Result<QuickTask> = runCatching {
        api.createQuickTask(CreateQuickTaskRequest(title = title)).toDomain()
    }

    suspend fun complete(id: String): Result<QuickTask> = runCatching {
        api.completeQuickTask(id).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deleteQuickTask(id)
    }
}
