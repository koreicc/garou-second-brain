package com.secondbrain.di

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.api.ApiServiceImpl
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.QuickTaskRepository
import com.secondbrain.data.repository.TaskRepository

object AppModule {

    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/v1"

    private val apiService: ApiService by lazy {
        ApiServiceImpl(baseUrl = DEFAULT_BASE_URL)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(apiService)
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(apiService)
    }

    val quickTaskRepository: QuickTaskRepository by lazy {
        QuickTaskRepository(apiService)
    }

    val personRepository: PersonRepository by lazy {
        PersonRepository(apiService)
    }
}
