package com.secondbrain.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.domain.model.Person
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PersonDetailUiState(
    val person: Person? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PersonDetailViewModel(
    private val personRepository: PersonRepository,
    private val personId: String
) : ViewModel() {

    private val _state = MutableStateFlow(PersonDetailUiState())
    val state: StateFlow<PersonDetailUiState> = _state.asStateFlow()

    init {
        loadPerson()
    }

    /**
     * Reloads the person data without showing a loading indicator.
     * Called when the screen resumes to reflect edits made elsewhere.
     */
    fun reload() {
        viewModelScope.launch {
            personRepository.get(personId)
                .onSuccess { person -> _state.update { it.copy(person = person, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadPerson() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            personRepository.get(personId)
                .onSuccess { person -> _state.update { it.copy(person = person, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
