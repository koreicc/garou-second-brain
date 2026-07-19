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
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

sealed interface SearchEvent {
    data class UpdateQuery(val query: String) : SearchEvent
    data object Search : SearchEvent
    data object ClearResults : SearchEvent
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
            }
            is SearchEvent.Search -> performSearch()
            is SearchEvent.ClearResults -> {
                _state.update { it.copy(results = emptyList(), hasSearched = false) }
            }
        }
    }

    private fun performSearch() {
        val query = _state.value.query.trim()
        if (query.isEmpty()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            searchRepository.search(query)
                .onSuccess { results ->
                    _state.update { it.copy(results = results, isLoading = false, hasSearched = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message, hasSearched = true) }
                }
        }
    }
}
