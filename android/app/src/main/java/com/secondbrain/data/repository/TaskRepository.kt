package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.*
import com.secondbrain.domain.model.Task

class TaskRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Task>> = runCatching {
        api.getAllTasks().map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<Task> = runCatching {
        api.getTask(id).toDomain()
    }

    suspend fun create(
        title: String,
        icon: String = "",
        location: String = "",
        tags: List<String> = emptyList(),
        startDate: String = "",
        endDate: String = "",
        recurrence: com.secondbrain.domain.model.Recurrence? = null,
        subtasks: List<com.secondbrain.domain.model.Subtask> = emptyList(),
        content: String = ""
    ): Result<Task> = runCatching {
        api.createTask(
            CreateTaskRequest(
                title = title,
                icon = icon,
                location = location,
                tags = tags,
                startDate = startDate,
                endDate = endDate,
                recurrence = recurrence?.toDto(),
                subtasks = subtasks.map { it.toDto() },
                content = content
            )
        ).toDomain()
    }

    suspend fun update(
        id: String,
        title: String? = null,
        status: String? = null,
        icon: String? = null,
        location: String? = null,
        tags: List<String>? = null,
        startDate: String? = null,
        endDate: String? = null,
        recurrence: com.secondbrain.domain.model.Recurrence? = null,
        subtasks: List<com.secondbrain.domain.model.Subtask>? = null,
        content: String? = null
    ): Result<Task> = runCatching {
        api.updateTask(
            id,
            UpdateTaskRequest(
                title = title,
                status = status,
                icon = icon,
                location = location,
                tags = tags,
                startDate = startDate,
                endDate = endDate,
                recurrence = recurrence?.toDto(),
                subtasks = subtasks?.map { it.toDto() },
                content = content
            )
        ).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deleteTask(id)
    }
}
