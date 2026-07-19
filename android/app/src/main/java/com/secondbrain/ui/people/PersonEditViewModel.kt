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
    val tagsInput: String = "",
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

sealed interface PersonEditEvent {
    data class UpdateName(val name: String) : PersonEditEvent
    data class UpdatePhone(val phone: String) : PersonEditEvent
    data class UpdateEmail(val email: String) : PersonEditEvent
    data class UpdateTags(val tags: String) : PersonEditEvent
    data class UpdateNotes(val notes: String) : PersonEditEvent
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
                            tagsInput = person.tags.joinToString(", "),
                            notes = person.notes,
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
            is PersonEditEvent.UpdateTags -> _state.update { it.copy(tagsInput = event.tags) }
            is PersonEditEvent.UpdateNotes -> _state.update { it.copy(notes = event.notes) }
            is PersonEditEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.name.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tags = s.tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val contacts = buildList {
                if (s.phone.isNotBlank()) add(ContactDto(type = "phone", value = s.phone, label = "Personal"))
                if (s.email.isNotBlank()) add(ContactDto(type = "email", value = s.email, label = "Personal"))
            }

            val result = if (personId != null) {
                personRepository.update(personId, UpdatePersonRequest(
                    name = s.name,
                    tags = tags,
                    contacts = contacts,
                    notes = s.notes
                ))
            } else {
                personRepository.create(CreatePersonRequest(
                    name = s.name,
                    tags = tags,
                    contacts = contacts,
                    notes = s.notes
                ))
            }

            result
                .onSuccess { _state.update { it.copy(isLoading = false, isSaved = true) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
