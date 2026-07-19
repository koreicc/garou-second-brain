package com.secondbrain.data.repository

import com.secondbrain.data.api.ApiService
import com.secondbrain.data.dto.SearchResultDto
import com.secondbrain.domain.model.SearchResult

class SearchRepository(private val api: ApiService) {

    suspend fun search(query: String): Result<List<SearchResult>> = runCatching {
        val response = api.search(query)
        if (response.error.isNotBlank()) throw Exception(response.error)
        (response.data ?: emptyList()).map { it.toDomain() }
    }
}

fun SearchResultDto.toDomain(): SearchResult = SearchResult(
    id = id,
    type = type,
    title = title,
    snippet = snippet
)
