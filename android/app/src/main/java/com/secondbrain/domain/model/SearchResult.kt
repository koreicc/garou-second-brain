package com.secondbrain.domain.model

data class SearchResult(
    val id: String,
    val type: String,
    val title: String,
    val snippet: String = ""
)
