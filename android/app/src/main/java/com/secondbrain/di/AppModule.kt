package com.secondbrain.di

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.api.KtorApiService
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.QuickTaskRepository
import com.secondbrain.data.repository.SearchRepository
import com.secondbrain.data.repository.TaskRepository
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object AppModule {

    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/api/v1"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient: HttpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    private val apiService: ApiService by lazy {
        KtorApiService(client = httpClient, baseUrl = DEFAULT_BASE_URL)
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

    val searchRepository: SearchRepository by lazy {
        SearchRepository(apiService)
    }
}
