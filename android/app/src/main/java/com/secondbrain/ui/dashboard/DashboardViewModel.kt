package com.secondbrain.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.QuickTaskRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val noteCount: Int = 0,
    val taskCount: Int = 0,
    val personCount: Int = 0,
    val recentNotes: List<Note> = emptyList(),
    val recentTasks: List<Task> = emptyList(),
    val quickTasks: List<QuickTask> = emptyList(),
    /** Quick tasks that are completed and awaiting deletion with countdown */
    val completingQuickTasks: Map<String, Int> = emptyMap(),
    /** IDs of recently completed tasks to filter out on reload until backend deletes them */
    val hiddenQuickTaskIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val quickTaskInput: String = ""
)

sealed interface DashboardEvent {
    data object LoadData : DashboardEvent
    data class UpdateQuickTaskInput(val input: String) : DashboardEvent
    data class CreateQuickTask(val title: String) : DashboardEvent
    data class CompleteQuickTask(val id: String) : DashboardEvent
    data class DeleteQuickTask(val id: String) : DashboardEvent
    data class DismissError(val message: String) : DashboardEvent
}

class DashboardViewModel(
    private val noteRepository: NoteRepository,
    private val taskRepository: TaskRepository,
    private val quickTaskRepository: QuickTaskRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LoadData -> loadData()
            is DashboardEvent.UpdateQuickTaskInput -> {
                _state.update { it.copy(quickTaskInput = event.input) }
            }
            is DashboardEvent.CreateQuickTask -> createQuickTask(event.title)
            is DashboardEvent.CompleteQuickTask -> completeQuickTask(event.id)
            is DashboardEvent.DeleteQuickTask -> deleteQuickTask(event.id)
            is DashboardEvent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Silently reloads data without showing loading indicator.
     * Used by RefreshOnResume to avoid flicker.
     */
    fun silentReload() {
        viewModelScope.launch {
            val hidden = _state.value.hiddenQuickTaskIds
            val loads = listOf(
                launch {
                    noteRepository.getAll().onSuccess { notes ->
                        _state.update { it.copy(noteCount = notes.size, recentNotes = notes.take(5)) }
                    }
                },
                launch {
                    taskRepository.getAll().onSuccess { tasks ->
                        _state.update { it.copy(taskCount = tasks.size, recentTasks = tasks.take(5)) }
                    }
                },
                launch {
                    personRepository.getAll().onSuccess { people ->
                        _state.update { it.copy(personCount = people.size) }
                    }
                },
                launch {
                    quickTaskRepository.getAll().onSuccess { quickTasks ->
                        _state.update { it.copy(quickTasks = quickTasks.filter { qt -> qt.id !in hidden }) }
                    }
                }
            )
            loads.forEach { it.join() }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val hidden = _state.value.hiddenQuickTaskIds
            val loads = listOf(
                launch {
                    noteRepository.getAll().onSuccess { notes ->
                        _state.update { it.copy(noteCount = notes.size, recentNotes = notes.take(5)) }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                },
                launch {
                    taskRepository.getAll().onSuccess { tasks ->
                        _state.update { it.copy(taskCount = tasks.size, recentTasks = tasks.take(5)) }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                },
                launch {
                    personRepository.getAll().onSuccess { people ->
                        _state.update { it.copy(personCount = people.size) }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                },
                launch {
                    quickTaskRepository.getAll().onSuccess { quickTasks ->
                        _state.update { it.copy(quickTasks = quickTasks.filter { qt -> qt.id !in hidden }) }
                    }.onFailure { e ->
                        _state.update { it.copy(error = e.message) }
                    }
                }
            )
            loads.forEach { it.join() }
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun createQuickTask(title: String) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) return

        viewModelScope.launch {
            quickTaskRepository.create(trimmedTitle).onSuccess {
                _state.update { state ->
                    state.copy(quickTaskInput = "")
                }
                loadData()
            }.onFailure { e ->
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    private fun completeQuickTask(id: String) {
        viewModelScope.launch {
            // Show countdown while keeping task visible
            _state.update {
                it.copy(
                    completingQuickTasks = it.completingQuickTasks + (id to 5),
                    hiddenQuickTaskIds = it.hiddenQuickTaskIds + id
                )
            }

            // Countdown from 5 to 1 (visible on the task card)
            for (remaining in 4 downTo 1) {
                delay(1000)
                _state.update {
                    it.copy(completingQuickTasks = it.completingQuickTasks + (id to remaining))
                }
            }

            // Wait last second
            delay(1000)

            // Mark as completed on backend
            quickTaskRepository.complete(id).onSuccess {
                // Remove from quickTasks list and keep in hidden set
                _state.update {
                    it.copy(
                        quickTasks = it.quickTasks.filter { qt -> qt.id != id },
                        completingQuickTasks = it.completingQuickTasks - id
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        error = e.message,
                        completingQuickTasks = it.completingQuickTasks - id,
                        hiddenQuickTaskIds = it.hiddenQuickTaskIds - id
                    )
                }
            }

            // Clean up hidden ID after enough time for backend auto-delete
            delay(8000)
            _state.update {
                it.copy(hiddenQuickTaskIds = it.hiddenQuickTaskIds - id)
            }
        }
    }

    private fun deleteQuickTask(id: String) {
        viewModelScope.launch {
            // Optimistic: remove from list immediately
            _state.update {
                it.copy(quickTasks = it.quickTasks.filter { qt -> qt.id != id })
            }

            quickTaskRepository.delete(id).onFailure { e ->
                _state.update { it.copy(error = e.message) }
                loadData()
            }
        }
    }
}
