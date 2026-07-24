package com.secondbrain.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val groupedTasks: Map<Int, List<Task>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Delete dialog
    val showDeleteDialog: Boolean = false,
    val pendingDeleteTask: Task? = null,
    // Filter/Sort
    val statusFilter: String = "",
    val priorityFilter: String = "",
    val searchQuery: String = "",
    val sortBy: String = "sort_key",
    val sortOrder: String = "asc",
    // Selection mode for batch ops
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    // Batch operation state
    val isBatchLoading: Boolean = false
)

sealed interface TaskListEvent {
    data object LoadTasks : TaskListEvent
    data class DeleteTask(val id: String) : TaskListEvent
    data class ShowDeleteConfirmation(val task: Task) : TaskListEvent
    data object DismissDelete : TaskListEvent
    data object ConfirmDelete : TaskListEvent
    // Filtering/Sorting
    data class SetStatusFilter(val status: String) : TaskListEvent
    data class SetPriorityFilter(val priority: String) : TaskListEvent
    data class SetSearchQuery(val query: String) : TaskListEvent
    data class SetSortBy(val sortBy: String) : TaskListEvent
    data object ToggleSortOrder : TaskListEvent
    // Selection mode
    data object ToggleSelectionMode : TaskListEvent
    data class ToggleSelection(val id: String) : TaskListEvent
    data object SelectAll : TaskListEvent
    data object ClearSelection : TaskListEvent
    // Batch operations
    data object BatchComplete : TaskListEvent
    data object BatchDelete : TaskListEvent
}

class TaskListViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListUiState())
    val state: StateFlow<TaskListUiState> = _state.asStateFlow()

    init {
        loadTasks()
    }

    fun onEvent(event: TaskListEvent) {
        when (event) {
            is TaskListEvent.LoadTasks -> loadTasks()
            is TaskListEvent.DeleteTask -> deleteTask(event.id)
            is TaskListEvent.ShowDeleteConfirmation -> {
                _state.update { it.copy(showDeleteDialog = true, pendingDeleteTask = event.task) }
            }
            is TaskListEvent.DismissDelete -> {
                _state.update { it.copy(showDeleteDialog = false, pendingDeleteTask = null) }
            }
            is TaskListEvent.ConfirmDelete -> {
                val task = _state.value.pendingDeleteTask ?: return
                _state.update { it.copy(showDeleteDialog = false) }
                deleteTask(task.id)
            }
            is TaskListEvent.SetStatusFilter -> {
                _state.update { it.copy(statusFilter = event.status) }
                loadTasks()
            }
            is TaskListEvent.SetPriorityFilter -> {
                _state.update { it.copy(priorityFilter = event.priority) }
                loadTasks()
            }
            is TaskListEvent.SetSearchQuery -> {
                _state.update { it.copy(searchQuery = event.query) }
                loadTasks()
            }
            is TaskListEvent.SetSortBy -> {
                _state.update { it.copy(sortBy = event.sortBy) }
                loadTasks()
            }
            is TaskListEvent.ToggleSortOrder -> {
                val newOrder = if (_state.value.sortOrder == "asc") "desc" else "asc"
                _state.update { it.copy(sortOrder = newOrder) }
                loadTasks()
            }
            is TaskListEvent.ToggleSelectionMode -> {
                _state.update {
                    it.copy(
                        isSelectionMode = !it.isSelectionMode,
                        selectedIds = emptySet()
                    )
                }
            }
            is TaskListEvent.ToggleSelection -> {
                _state.update {
                    val newSelected = if (event.id in it.selectedIds) {
                        it.selectedIds - event.id
                    } else {
                        it.selectedIds + event.id
                    }
                    it.copy(selectedIds = newSelected)
                }
            }
            is TaskListEvent.SelectAll -> {
                _state.update {
                    it.copy(selectedIds = it.tasks.map { t -> t.id }.toSet())
                }
            }
            is TaskListEvent.ClearSelection -> {
                _state.update { it.copy(selectedIds = emptySet()) }
            }
            is TaskListEvent.BatchComplete -> batchOperation("complete")
            is TaskListEvent.BatchDelete -> batchOperation("delete")
        }
    }

    private fun filterStandalone(tasks: List<Task>): List<Task> {
        return tasks.filter { it.parentId.isEmpty() }
    }

    private fun groupByTimeBucket(tasks: List<Task>): Map<Int, List<Task>> {
        return tasks.groupBy { it.timeBucket }
    }

    fun silentReload() {
        viewModelScope.launch {
            val s = _state.value
            taskRepository.getAll(
                status = s.statusFilter.ifBlank { null },
                priority = s.priorityFilter.ifBlank { null },
                search = s.searchQuery.ifBlank { null },
                sortBy = s.sortBy,
                sortOrder = s.sortOrder
            )
                .onSuccess { tasks ->
                    val filtered = filterStandalone(tasks)
                    _state.update { it.copy(tasks = filtered, groupedTasks = groupByTimeBucket(filtered), error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            taskRepository.getAll(
                status = s.statusFilter.ifBlank { null },
                priority = s.priorityFilter.ifBlank { null },
                search = s.searchQuery.ifBlank { null },
                sortBy = s.sortBy,
                sortOrder = s.sortOrder
            )
                .onSuccess { tasks ->
                    val filtered = filterStandalone(tasks)
                    _state.update { it.copy(tasks = filtered, groupedTasks = groupByTimeBucket(filtered), isLoading = false) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun deleteTask(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(tasks = it.tasks.filter { t -> t.id != id }) }
            taskRepository.delete(id)
                .onSuccess { silentReload() }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    silentReload()
                }
        }
    }

    private fun batchOperation(action: String) {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isBatchLoading = true) }
            taskRepository.batch(ids, action)
                .onSuccess {
                    _state.update {
                        it.copy(
                            isBatchLoading = false,
                            isSelectionMode = false,
                            selectedIds = emptySet()
                        )
                    }
                    silentReload()
                }
                .onFailure { e ->
                    _state.update { it.copy(isBatchLoading = false, error = e.message) }
                }
        }
    }
}