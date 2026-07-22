package com.secondbrain.data.repository

import com.secondbrain.domain.model.LinkedEntityInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class LinkingRepository(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val personRepository: PersonRepository
) {
    suspend fun resolveLinks(ids: List<String>): Result<List<LinkedEntityInfo>> = runCatching {
        if (ids.isEmpty()) return@runCatching emptyList()
        val idSet = ids.toSet()
        coroutineScope {
            val notesDeferred = async { noteRepository.getAll().getOrNull() ?: emptyList() }
            val tasksDeferred = async { taskRepository.getAll().getOrNull() ?: emptyList() }
            val peopleDeferred = async { personRepository.getAll().getOrNull() ?: emptyList() }

            val notes = notesDeferred.await()
            val tasks = tasksDeferred.await()
            val people = peopleDeferred.await()

            val results = mutableListOf<LinkedEntityInfo>()
            for (note in notes) {
                if (note.id in idSet) {
                    results.add(LinkedEntityInfo(id = note.id, type = "note", title = note.title, status = note.status))
                }
            }
            for (task in tasks) {
                if (task.id in idSet) {
                    results.add(LinkedEntityInfo(id = task.id, type = "task", title = task.title, status = task.status))
                }
            }
            for (person in people) {
                if (person.id in idSet) {
                    results.add(LinkedEntityInfo(id = person.id, type = "person", title = person.name, status = person.status))
                }
            }
            results
        }
    }

    suspend fun getAllLinkableEntities(): Result<List<LinkedEntityInfo>> = runCatching {
        coroutineScope {
            val notesDeferred = async { noteRepository.getAll().getOrNull() ?: emptyList() }
            val tasksDeferred = async { taskRepository.getAll().getOrNull() ?: emptyList() }
            val peopleDeferred = async { personRepository.getAll().getOrNull() ?: emptyList() }

            val notes = notesDeferred.await()
            val tasks = tasksDeferred.await()
            val people = peopleDeferred.await()

            val results = mutableListOf<LinkedEntityInfo>()
            notes.forEach { results.add(LinkedEntityInfo(id = it.id, type = "note", title = it.title, status = it.status)) }
            tasks.forEach { results.add(LinkedEntityInfo(id = it.id, type = "task", title = it.title, status = it.status)) }
            people.forEach { results.add(LinkedEntityInfo(id = it.id, type = "person", title = it.name, status = it.status)) }
            results
        }
    }
}
