package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreateHabitRequest
import com.secondbrain.data.dto.UpdateHabitRequest
import com.secondbrain.data.dto.toDomain
import com.secondbrain.domain.model.Habit

class HabitRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Habit>> = runCatching {
        val response = api.getAllHabits()
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun get(id: String): Result<Habit> = runCatching {
        val response = api.getHabit(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Habit not found")).toDomain()
    }

    suspend fun create(request: CreateHabitRequest): Result<Habit> = runCatching {
        val response = api.createHabit(request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun update(id: String, request: UpdateHabitRequest): Result<Habit> = runCatching {
        val response = api.updateHabit(id, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val response = api.deleteHabit(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
    }

    suspend fun complete(id: String): Result<Habit> = runCatching {
        val response = api.completeHabit(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun getToday(): Result<List<Habit>> = runCatching {
        val response = api.getTodayHabits()
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }
}
