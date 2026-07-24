package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.domain.model.LinkedEntityInfo

class LinkingRepository(private val api: ApiService) {
    suspend fun resolveLinks(ids: List<String>): Result<List<LinkedEntityInfo>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyList()
        val response = api.getEntitiesByIds(ids)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map {
            LinkedEntityInfo(id = it.id, type = it.type, title = it.title, status = it.status)
        }
    }

    suspend fun getAllLinkableEntities(): Result<List<LinkedEntityInfo>> = runCatching {
        // For the link picker, we still need all entities.
        // This is only called from edit screens, not detail screens.
        val notesResponse = api.getAllNotes()
        val tasksResponse = api.getAllTasks()
        val peopleResponse = api.getAllPeople()

        val results = mutableListOf<LinkedEntityInfo>()
        (notesResponse.data ?: emptyList()).forEach {
            results.add(LinkedEntityInfo(id = it.id, type = "note", title = it.title, status = it.status))
        }
        (tasksResponse.data ?: emptyList()).forEach {
            results.add(LinkedEntityInfo(id = it.id, type = "task", title = it.title, status = it.status))
        }
        (peopleResponse.data ?: emptyList()).forEach {
            results.add(LinkedEntityInfo(id = it.id, type = "person", title = it.name, status = it.status))
        }
        results
    }
}
