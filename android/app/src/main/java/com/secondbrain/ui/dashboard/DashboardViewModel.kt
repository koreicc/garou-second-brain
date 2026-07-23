package com.secondbrain.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.dto.CreateNoteRequest
import com.secondbrain.data.dto.UpdateTaskRequest
import com.secondbrain.data.dto.toDto
import com.secondbrain.data.repository.NoteRepository
import com.secondbrain.data.repository.PersonRepository
import com.secondbrain.data.repository.QuickTaskRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RoutineInfo(
    val task: Task,
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val isComplete: Boolean = false
)

data class DashboardUiState(
    // Greeting
    val greeting: String = "",
    val dateString: String = "",
    // Routine
    val routine: RoutineInfo? = null,
    val routineTimeOfDay: String = "",
    // Today's tasks
    val todayTasks: List<Task> = emptyList(),
    // Quick tasks
    val quickTasks: List<QuickTask> = emptyList(),
    val completingQuickTasks: Map<String, Int> = emptyMap(),
    val hiddenQuickTaskIds: Set<String> = emptySet(),
    // Quick note
    val quickNoteTitle: String = "",
    val quickNoteContent: String = "",
    // Loading
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface DashboardEvent {
    data object LoadData : DashboardEvent
    // Routine
    data class ToggleRoutineSubtask(val subtaskId: String) : DashboardEvent
    data object CompleteRoutine : DashboardEvent
    // Quick tasks
    data class CreateQuickTask(val title: String) : DashboardEvent
    data class CompleteQuickTask(val id: String) : DashboardEvent
    data class DeleteQuickTask(val id: String) : DashboardEvent
    // Quick note
    data class UpdateQuickNoteTitle(val title: String) : DashboardEvent
    data class UpdateQuickNoteContent(val content: String) : DashboardEvent
    data object CreateQuickNote : DashboardEvent
    // Error
    data object DismissError : DashboardEvent
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
            is DashboardEvent.ToggleRoutineSubtask -> toggleRoutineSubtask(event.subtaskId)
            is DashboardEvent.CompleteRoutine -> completeRoutine()
            is DashboardEvent.CreateQuickTask -> createQuickTask(event.title)
            is DashboardEvent.CompleteQuickTask -> completeQuickTask(event.id)
            is DashboardEvent.DeleteQuickTask -> deleteQuickTask(event.id)
            is DashboardEvent.UpdateQuickNoteTitle -> _state.update { it.copy(quickNoteTitle = event.title) }
            is DashboardEvent.UpdateQuickNoteContent -> _state.update { it.copy(quickNoteContent = event.content) }
            is DashboardEvent.CreateQuickNote -> createQuickNote()
            is DashboardEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    fun silentReload() {
        viewModelScope.launch {
            loadDataInternal(isSilent = true)
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadDataInternal(isSilent = false)
        }
    }

    private suspend fun loadDataInternal(isSilent: Boolean) {
        val now = LocalDateTime.now()
        val hour = now.hour
        val greeting = when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
        val dateStr = now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

        val routineTime = when (hour) {
            in 5..11 -> "morning-routine"
            in 17..21 -> "evening-routine"
            else -> null
        }

        val hidden = _state.value.hiddenQuickTaskIds

        coroutineScope {
            val tasksDeferred = async { taskRepository.getAll().getOrDefault(emptyList()) }
            val quickTasksDeferred = async { quickTaskRepository.getAll().getOrDefault(emptyList()) }

            val allTasks = tasksDeferred.await()

            val routineInfo = routineTime?.let { timeTag ->
                allTasks
                    .filter { task ->
                        task.tags.any { tag -> tag == "routine" } &&
                            task.tags.any { tag -> tag == timeTag } &&
                            task.status != "expired"
                    }
                    .maxByOrNull { task -> task.updatedAt }
                    ?.let { task ->
                        val total = task.subtasks.size
                        val completed = task.subtasks.count { it.completed }
                        RoutineInfo(
                            task = task,
                            completedCount = completed,
                            totalCount = total,
                            isComplete = total > 0 && completed == total
                        )
                    }
            }

            val today = LocalDate.now()
            val todayStr = today.toString()
            val todayTasks = allTasks
                .filter { task ->
                    task.status == "pending" || task.status == "in-progress"
                }
                .filter { task ->
                    when (task.dateMode) {
                        "due_date" -> {
                            // Show if due date is today or overdue (not yet completed)
                            val due = task.dueDate.take(10)
                            due.isNotEmpty() && due <= todayStr
                        }
                        "range" -> {
                            val start = task.startDate.take(10)
                            val end = task.endDate.take(10)
                            start.isNotEmpty() && end.isNotEmpty() && start <= todayStr && end >= todayStr ||
                                start.isNotEmpty() && end.isEmpty() && start <= todayStr ||
                                start.isEmpty() && end.isNotEmpty() && end >= todayStr
                        }
                        else -> {
                            // No date mode - still show in today's tasks
                            true
                        }
                    }
                }
                .filter { task -> !task.tags.contains("routine") }
                .sortedWith(compareBy<Task> { 
                    // Tasks with due dates/end dates first, sorted by date
                    when (it.dateMode) {
                        "due_date" -> it.dueDate.take(10)
                        "range" -> it.endDate.take(10)
                        else -> ""
                    }
                }.thenBy { it.title })
                .take(5)

            val quickTasks = quickTasksDeferred.await().filter { qt -> qt.id !in hidden }

            _state.update {
                it.copy(
                    greeting = greeting,
                    dateString = dateStr,
                    routine = routineInfo,
                    routineTimeOfDay = routineTime?.removeSuffix("-routine") ?: "",
                    todayTasks = todayTasks,
                    quickTasks = quickTasks,
                    isLoading = if (!isSilent) false else it.isLoading
                )
            }
        }
    }

    private fun toggleRoutineSubtask(subtaskId: String) {
        val routine = _state.value.routine ?: return
        val task = routine.task
        val updatedSubtasks = task.subtasks.map { sub ->
            if (sub.id == subtaskId) sub.copy(completed = !sub.completed) else sub
        }

        viewModelScope.launch {
            val subtaskDtos = updatedSubtasks.map { it.toDto() }
            taskRepository.update(task.id, UpdateTaskRequest(subtasks = subtaskDtos))
                .onSuccess {
                    silentReload()
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun completeRoutine() {
        val routine = _state.value.routine ?: return
        val task = routine.task
        val updatedSubtasks = task.subtasks.map { it.copy(completed = true) }

        viewModelScope.launch {
            val subtaskDtos = updatedSubtasks.map { it.toDto() }
            taskRepository.update(task.id, UpdateTaskRequest(
                subtasks = subtaskDtos,
                status = "completed"
            ))
                .onSuccess {
                    silentReload()
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }

    private fun createQuickTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            quickTaskRepository.create(trimmed)
                .onSuccess { silentReload() }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
        }
    }

    private fun completeQuickTask(id: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    completingQuickTasks = it.completingQuickTasks + (id to 5),
                    hiddenQuickTaskIds = it.hiddenQuickTaskIds + id
                )
            }
            for (remaining in 4 downTo 1) {
                delay(1000)
                _state.update {
                    it.copy(completingQuickTasks = it.completingQuickTasks + (id to remaining))
                }
            }
            delay(1000)
            quickTaskRepository.complete(id)
                .onSuccess {
                    _state.update {
                        it.copy(
                            quickTasks = it.quickTasks.filter { qt -> qt.id != id },
                            completingQuickTasks = it.completingQuickTasks - id
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            error = e.message,
                            completingQuickTasks = it.completingQuickTasks - id,
                            hiddenQuickTaskIds = it.hiddenQuickTaskIds - id
                        )
                    }
                }
            delay(8000)
            _state.update { it.copy(hiddenQuickTaskIds = it.hiddenQuickTaskIds - id) }
        }
    }

    private fun deleteQuickTask(id: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(quickTasks = it.quickTasks.filter { qt -> qt.id != id })
            }
            quickTaskRepository.delete(id)
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                    loadData()
                }
        }
    }

    private fun createQuickNote() {
        val title = _state.value.quickNoteTitle.trim()
        val content = _state.value.quickNoteContent.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            noteRepository.create(CreateNoteRequest(title = title, body = content))
                .onSuccess {
                    _state.update { it.copy(quickNoteTitle = "", quickNoteContent = "") }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message) }
                }
        }
    }
}

private fun <T> Result<T>.getOrDefault(default: T): T = getOrNull() ?: default
