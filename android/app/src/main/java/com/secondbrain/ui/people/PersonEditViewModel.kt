package com.secondbrain.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.*
import com.secondbrain.data.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonEditUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val socialLinks: List<SocialLinkItem> = emptyList(),
    val newPlatform: String = "",
    val newUrl: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

data class SocialLinkItem(
    val platform: String,
    val url: String
)

sealed interface PersonEditEvent {
    data class UpdateName(val name: String) : PersonEditEvent
    data class UpdatePhone(val phone: String) : PersonEditEvent
    data class UpdateEmail(val email: String) : PersonEditEvent
    data class SetTags(val tags: List<String>) : PersonEditEvent
    data class UpdateNotes(val notes: String) : PersonEditEvent
    data class UpdateNewPlatform(val platform: String) : PersonEditEvent
    data class UpdateNewUrl(val url: String) : PersonEditEvent
    data object AddSocialLink : PersonEditEvent
    data class RemoveSocialLink(val index: Int) : PersonEditEvent
    data object Save : PersonEditEvent
}

class PersonEditViewModel(
    private val personRepository: PersonRepository,
    private val personId: String?
) : ViewModel() {

    private val _state = MutableStateFlow(PersonEditUiState())
    val state: StateFlow<PersonEditUiState> = _state.asStateFlow()

    init {
        if (personId != null) {
            loadPerson()
        }
    }

    private fun loadPerson() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            personRepository.get(personId!!)
                .onSuccess { person ->
                    val phone = person.contacts.find { it.type == "phone" }?.value ?: ""
                    val email = person.contacts.find { it.type == "email" }?.value ?: ""
                    _state.update {
                        it.copy(
                            name = person.name,
                            phone = phone,
                            email = email,
                            tags = person.tags,
                            notes = person.notes,
                            socialLinks = person.socialLinks.map { SocialLinkItem(platform = it.platform, url = it.url) },
                            isLoading = false
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun onEvent(event: PersonEditEvent) {
        when (event) {
            is PersonEditEvent.UpdateName -> _state.update { it.copy(name = event.name) }
            is PersonEditEvent.UpdatePhone -> _state.update { it.copy(phone = event.phone) }
            is PersonEditEvent.UpdateEmail -> _state.update { it.copy(email = event.email) }
            is PersonEditEvent.SetTags -> _state.update { it.copy(tags = event.tags) }
            is PersonEditEvent.UpdateNotes -> _state.update { it.copy(notes = event.notes) }
            is PersonEditEvent.UpdateNewPlatform -> _state.update { it.copy(newPlatform = event.platform) }
            is PersonEditEvent.UpdateNewUrl -> _state.update { it.copy(newUrl = event.url) }
            is PersonEditEvent.AddSocialLink -> addSocialLink()
            is PersonEditEvent.RemoveSocialLink -> removeSocialLink(event.index)
            is PersonEditEvent.Save -> save()
        }
    }

    private fun addSocialLink() {
        val platform = _state.value.newPlatform.trim()
        val url = _state.value.newUrl.trim()
        if (platform.isEmpty() || url.isEmpty()) return
        _state.update {
            it.copy(
                socialLinks = it.socialLinks + SocialLinkItem(platform = platform, url = url),
                newPlatform = "",
                newUrl = ""
            )
        }
    }

    private fun removeSocialLink(index: Int) {
        _state.update {
            it.copy(socialLinks = it.socialLinks.toMutableList().also { list -> list.removeAt(index) })
        }
    }

    private fun save() {
        val s = _state.value
        if (s.name.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val contacts = buildList {
                if (s.phone.isNotBlank()) add(ContactDto(type = "phone", value = s.phone, label = "Personal"))
                if (s.email.isNotBlank()) add(ContactDto(type = "email", value = s.email, label = "Personal"))
            }
            val socialLinkDtos = s.socialLinks.map { SocialLinkDto(platform = it.platform, url = it.url) }

            val result = if (personId != null) {
                personRepository.update(personId, UpdatePersonRequest(
                    name = s.name,
                    tags = if (s.tags.isNotEmpty()) s.tags else null,
                    contacts = if (contacts.isNotEmpty()) contacts else null,
                    socialLinks = if (socialLinkDtos.isNotEmpty()) socialLinkDtos else null,
                    notes = s.notes
                ))
            } else {
                personRepository.create(CreatePersonRequest(
                    name = s.name,
                    tags = s.tags,
                    contacts = contacts,
                    socialLinks = socialLinkDtos,
                    notes = s.notes
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
