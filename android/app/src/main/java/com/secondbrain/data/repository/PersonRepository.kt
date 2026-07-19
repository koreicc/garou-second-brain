package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.CreatePersonRequest
import com.secondbrain.data.dto.UpdatePersonRequest
import com.secondbrain.domain.model.Person

class PersonRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Person>> = runCatching {
        val response = api.getAllPeople()
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }

    suspend fun get(id: String): Result<Person> = runCatching {
        val response = api.getPerson(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Person not found")).toDomain()
    }

    suspend fun create(request: CreatePersonRequest): Result<Person> = runCatching {
        val response = api.createPerson(request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun update(id: String, request: UpdatePersonRequest): Result<Person> = runCatching {
        val response = api.updatePerson(id, request)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: throw Exception("Empty response")).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        val response = api.deletePerson(id)
        if (response.error.isNotBlank()) throw Exception(response.error)
    }
}
