package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.ApiResponse
import com.secondbrain.domain.model.SearchResult

class SearchRepository(private val api: ApiService) {

    private fun <T> checkError(response: ApiResponse<T>): T {
        if (response.error.isNotBlank()) {
            throw Exception(response.error)
        }
        return response.data ?: throw Exception("Empty response")
    }

    suspend fun search(query: String): Result<List<SearchResult>> = runCatching {
        checkError(api.search(query)).map {
            SearchResult(id = it.id, type = it.type, title = it.title, snippet = it.snippet)
        }
    }
}
