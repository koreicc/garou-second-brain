package com.secondbrain.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.SearchRepository
import com.secondbrain.domain.model.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

sealed interface SearchEvent {
    data class UpdateQuery(val query: String) : SearchEvent
    data object Search : SearchEvent
    data object DismissError : SearchEvent
}

class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onEvent(event: SearchEvent) {
        when (event) {
            is SearchEvent.UpdateQuery -> {
                _state.update { it.copy(query = event.query) }
                // Debounce search
                searchJob?.cancel()
                if (event.query.length >= 2) {
                    searchJob = viewModelScope.launch {
                        delay(300)
                        performSearch(event.query)
                    }
                } else {
                    _state.update { it.copy(results = emptyList(), hasSearched = false) }
                }
            }
            is SearchEvent.Search -> performSearch(_state.value.query)
            is SearchEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }
            searchRepository.search(query.trim())
                .onSuccess { results ->
                    _state.update { it.copy(results = results, isSearching = false, hasSearched = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isSearching = false, error = e.message, hasSearched = true) }
                }
        }
    }
}