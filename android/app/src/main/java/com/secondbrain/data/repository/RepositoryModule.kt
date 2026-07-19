package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService

class RepositoryModule(private val api: ApiService) {

    val noteRepository: NoteRepository by lazy { NoteRepository(api) }
    val taskRepository: TaskRepository by lazy { TaskRepository(api) }
    val quickTaskRepository: QuickTaskRepository by lazy { QuickTaskRepository(api) }
    val personRepository: PersonRepository by lazy { PersonRepository(api) }
    val searchRepository: SearchRepository by lazy { SearchRepository(api) }
}
