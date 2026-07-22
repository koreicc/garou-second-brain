package com.secondbrain.domain.model

data class LinkedEntityInfo(
    val id: String,
    val type: String,   // "note", "task", "person"
    val title: String,
    val subtitle: String = "",
    val status: String = ""
)
