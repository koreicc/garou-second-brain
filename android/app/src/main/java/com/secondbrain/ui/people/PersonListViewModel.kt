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

data class PersonListUiState(
    val people: List<Person> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val pendingDeletePerson: Person? = null
)

sealed interface PersonListEvent {
    data object LoadPeople : PersonListEvent
    data class DeletePerson(val id: String) : PersonListEvent
    data class ShowDeleteConfirmation(val person: Person) : PersonListEvent
    data object DismissDelete : PersonListEvent
    data object ConfirmDelete : PersonListEvent
}

class PersonListViewModel(
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PersonListUiState())
    val state: StateFlow<PersonListUiState> = _state.asStateFlow()

    init {
        loadPeople()
    }

    fun onEvent(event: PersonListEvent) {
        when (event) {
            is PersonListEvent.LoadPeople -> loadPeople()
            is PersonListEvent.DeletePerson -> deletePerson(event.id)
            is PersonListEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(showDeleteDialog = true, pendingDeletePerson = event.person) }
            }
            is PersonListEvent.DismissDelete -> {
                _state.update { it.copy(showDeleteDialog = false, pendingDeletePerson = null) }
            }
            is PersonListEvent.ConfirmDelete -> {
                val person = _state.value.pendingDeletePerson ?: return
                _state.update { it.copy(showDeleteDialog = false) }
                deletePerson(person.id)
            }
        }
    }

    /**
     * Silently reloads without showing the loading indicator.
     */
    fun silentReload() {
        viewModelScope.launch {
            personRepository.getAll()
                .onSuccess { people ->
                    _state.update { it.copy(people = people, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun loadPeople() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            personRepository.getAll()
                .onSuccess { people -> _state.update { it.copy(people = people, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun deletePerson(id: String) {
        viewModelScope.launch {
            // Optimistic removal
            _state.update { it.copy(people = it.people.filter { p -> p.id != id }) }

            personRepository.delete(id)
                .onSuccess { silentReload() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    silentReload()
                }
        }
    }
}
