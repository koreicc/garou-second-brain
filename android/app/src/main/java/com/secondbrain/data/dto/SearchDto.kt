package com.secondbrain.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultDto(
    val id: String,
    val type: String,
    val title: String,
    val snippet: String = ""
)

@Serializable
data class WikilinkResponse(
    val type: String = "",
    val id: String = "",
    val title: String = ""
)
