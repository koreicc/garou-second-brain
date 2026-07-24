package com.secondbrain.domain.model

data class Person(
    val id: String,
    val type: String = "person",
    val name: String,
    val status: String = "active",
    val contacts: List<Contact> = emptyList(),
    val socialLinks: List<SocialLink> = emptyList(),
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val notes: String = "",
    val body: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class Contact(
    val type: String,  // phone, email, social
    val value: String,
    val label: String = ""
)

data class SocialLink(
    val platform: String,
    val url: String
)
