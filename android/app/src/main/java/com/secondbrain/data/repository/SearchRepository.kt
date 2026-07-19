package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.domain.model.SearchResult

class SearchRepository(private val api: ApiService) {

    suspend fun search(query: String): NetworkResult<List<SearchResult>> {
        return try {
            val response = api.search(query)
            if (response.error.isNotEmpty()) {
                NetworkResult.Error(response.error)
            } else {
                val results = response.data?.map {
                    SearchResult(id = it.id, type = it.type, title = it.title, snippet = it.snippet)
                } ?: emptyList()
                NetworkResult.Success(results)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "Search failed")
        }
    }
}
