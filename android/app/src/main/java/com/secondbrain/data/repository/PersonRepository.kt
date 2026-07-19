package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.*
import com.secondbrain.domain.model.Person

class PersonRepository(private val api: ApiService) {

    suspend fun getAll(): Result<List<Person>> = runCatching {
        api.getAllPeople().map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<Person> = runCatching {
        api.getPerson(id).toDomain()
    }

    suspend fun create(
        name: String,
        contacts: List<com.secondbrain.domain.model.Contact> = emptyList(),
        socialLinks: List<com.secondbrain.domain.model.SocialLink> = emptyList(),
        tags: List<String> = emptyList(),
        notes: String = ""
    ): Result<Person> = runCatching {
        api.createPerson(
            CreatePersonRequest(
                name = name,
                contacts = contacts.map { it.toDto() },
                socialLinks = socialLinks.map { it.toDto() },
                tags = tags,
                notes = notes
            )
        ).toDomain()
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
        api.updatePerson(
            id,
            UpdatePersonRequest(
                name = name,
                status = status,
                contacts = contacts?.map { it.toDto() },
                socialLinks = socialLinks?.map { it.toDto() },
                tags = tags,
                notes = notes
            )
        ).toDomain()
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        api.deletePerson(id)
    }
}
