package com.secondbrain.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.HabitRepository
import com.secondbrain.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HabitListUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val pendingDeleteHabit: Habit? = null,
    val isRefreshing: Boolean = false
)

sealed interface HabitListEvent {
    data object LoadHabits : HabitListEvent
    data class ShowDeleteConfirmation(val habit: Habit) : HabitListEvent
    data object DismissDelete : HabitListEvent
    data object ConfirmDelete : HabitListEvent
    data class CompleteHabit(val id: String) : HabitListEvent
}

class HabitListViewModel(
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HabitListUiState())
    val state: StateFlow<HabitListUiState> = _state.asStateFlow()

    init {
        loadHabits()
    }

    fun onEvent(event: HabitListEvent) {
        when (event) {
            is HabitListEvent.LoadHabits -> loadHabits()
            is HabitListEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(showDeleteDialog = true, pendingDeleteHabit = event.habit) }
            }
            is HabitListEvent.DismissDelete -> {
                _state.update { it.copy(showDeleteDialog = false, pendingDeleteHabit = null) }
            }
            is HabitListEvent.ConfirmDelete -> {
                val habit = _state.value.pendingDeleteHabit ?: return
                _state.update { it.copy(showDeleteDialog = false) }
                deleteHabit(habit.id)
            }
            is HabitListEvent.CompleteHabit -> completeHabit(event.id)
        }
    }

    fun silentReload() {
        viewModelScope.launch {
            habitRepository.getAll()
                .onSuccess { habits ->
                    _state.update { it.copy(habits = habits, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun loadHabits() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            habitRepository.getAll()
                .onSuccess { habits ->
                    _state.update { it.copy(habits = habits, isLoading = false) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            habitRepository.getAll()
                .onSuccess { habits -> _state.update { it.copy(habits = habits, isRefreshing = false) } }
                .onFailure { e -> _state.update { it.copy(isRefreshing = false, error = e.message) } }
        }
    }

    private fun deleteHabit(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(habits = it.habits.filter { h -> h.id != id }) }
            habitRepository.delete(id)
                .onSuccess { silentReload() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    silentReload()
                }
        }
    }

    private fun completeHabit(id: String) {
        viewModelScope.launch {
            habitRepository.complete(id)
                .onSuccess { completedHabit ->
                    _state.update {
                        it.copy(habits = it.habits.map { h ->
                            if (h.id == id) completedHabit else h
                        })
                    }
                }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }
}
