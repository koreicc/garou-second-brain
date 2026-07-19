package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.ApiResponse
import com.secondbrain.data.dto.CreatePersonRequest
import com.secondbrain.data.dto.UpdatePersonRequest
import com.secondbrain.domain.model.Person

class PersonRepository(private val api: ApiService) {

    private fun <T> checkError(response: ApiResponse<T>): T {
        if (response.error.isNotBlank()) {
            throw Exception(response.error)
        }
        return response.data ?: throw Exception("Empty response")
    }

    suspend fun getAll(): Result<List<Person>> = runCatching {
        checkError(api.getAllPeople()).map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<Person> = runCatching {
        checkError(api.getPerson(id)).toDomain()
    }

    suspend fun create(
        name: String,
        contacts: List<com.secondbrain.domain.model.Contact> = emptyList(),
        socialLinks: List<com.secondbrain.domain.model.SocialLink> = emptyList(),
        tags: List<String> = emptyList(),
        notes: String = ""
    ): Result<Person> = runCatching {
        checkError(api.createPerson(
            CreatePersonRequest(
                name = name,
                contacts = contacts.map { it.toDto() },
                socialLinks = socialLinks.map { it.toDto() },
                tags = tags,
                notes = notes
            )
        )).toDomain()
    }

    suspend fun update(
        id: String,
        name: String? = null,
        status: String? = null,
        contacts: List<com.secondbrain.domain.model.Contact>? = null,
        socialLinks: List<com.secondbrain.domain.model.SocialLink>? = null,
        tags: List<String>? = null,
        notes: String? = null
    ): Result<Person> = runCatching {
        checkError(api.updatePerson(
            id,
            UpdatePersonRequest(
                name = name,
                status = status,
                contacts = contacts?.map { it.toDto() },
                socialLinks = socialLinks?.map { it.toDto() },
                tags = tags,
                notes = notes
            )
        )).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        checkError(api.deletePerson(id))
    }
}
