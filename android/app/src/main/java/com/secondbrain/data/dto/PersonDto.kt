package com.secondbrain.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.secondbrain.domain.model.*

@Serializable
data class PersonDto(
    val id: String,
    val name: String,
    val status: String = "active",
    val contacts: List<ContactDto> = emptyList(),
    @SerialName("social_links")
    val socialLinks: List<SocialLinkDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val notes: String = "",
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class ContactDto(
    val type: String,
    val value: String,
    val label: String = ""
)

@Serializable
data class SocialLinkDto(
    val platform: String,
    val url: String
)

@Serializable
data class CreatePersonRequest(
    val name: String,
    val contacts: List<ContactDto> = emptyList(),
    @SerialName("social_links")
    val socialLinks: List<SocialLinkDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val links: List<String> = emptyList(),
    val notes: String = ""
)

@Serializable
data class UpdatePersonRequest(
    val name: String? = null,
    val status: String? = null,
    val contacts: List<ContactDto>? = null,
    @SerialName("social_links")
    val socialLinks: List<SocialLinkDto>? = null,
    val tags: List<String>? = null,
    val links: List<String>? = null,
    val notes: String? = null
)

fun PersonDto.toDomain(): Person = Person(
    id = id,
    name = name,
    status = status,
    contacts = contacts.map { it.toDomain() },
    socialLinks = socialLinks.map { it.toDomain() },
    tags = tags,
    links = links,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContactDto.toDomain(): Contact = Contact(
    type = type,
    value = value,
    label = label
)

fun SocialLinkDto.toDomain(): SocialLink = SocialLink(
    platform = platform,
    url = url
)

fun Contact.toDto(): ContactDto = ContactDto(
    type = type,
    value = value,
    label = label
)

fun SocialLink.toDto(): SocialLinkDto = SocialLinkDto(
    platform = platform,
    url = url
)
