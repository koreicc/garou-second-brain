package com.secondbrain.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val selectedDateTasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class CalendarViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadMonth(_state.value.currentMonth)
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        loadTasksForDate(date)
    }

    fun previousMonth() {
        val newMonth = _state.value.currentMonth.minusMonths(1)
        _state.update { it.copy(currentMonth = newMonth) }
        loadMonth(newMonth)
    }

    fun nextMonth() {
        val newMonth = _state.value.currentMonth.plusMonths(1)
        _state.update { it.copy(currentMonth = newMonth) }
        loadMonth(newMonth)
    }

    fun setMonth(month: YearMonth) {
        _state.update { it.copy(currentMonth = month) }
        loadMonth(month)
    }

    private fun loadMonth(month: YearMonth) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val allTasks = taskRepository.getAll().getOrDefault(emptyList())
            val nonTemplateTasks = allTasks.filter { !it.isTemplate }

            // Group tasks by their effective date
            val tasksByDate = mutableMapOf<LocalDate, MutableList<Task>>()
            val startOfMonth = month.atDay(1)
            val endOfMonth = month.atEndOfMonth()

            for (task in nonTemplateTasks) {
                val taskDates = getTaskDates(task, startOfMonth, endOfMonth)
                for (date in taskDates) {
                    tasksByDate.getOrPut(date) { mutableListOf() }.add(task)
                }
            }

            _state.update {
                it.copy(
                    tasksByDate = tasksByDate,
                    isLoading = false
                )
            }

            // Load tasks for the currently selected date
            loadTasksForDate(_state.value.selectedDate)
        }
    }

    private fun loadTasksForDate(date: LocalDate) {
        viewModelScope.launch {
            val result = taskRepository.getByDate(date.toString())
            val tasks = result.getOrDefault(emptyList()).filter { !it.isTemplate }
            _state.update { it.copy(selectedDateTasks = tasks) }
        }
    }

    /**
     * Returns the dates a task falls on within the given month range.
     */
    private fun getTaskDates(task: Task, monthStart: LocalDate, monthEnd: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()

        when (task.dateMode) {
            "due_date" -> {
                val dueDate = parseLocalDate(task.dueDate) ?: return dates
                if (dueDate in monthStart..monthEnd) {
                    dates.add(dueDate)
                }
            }
            "range" -> {
                val startDate = parseLocalDate(task.startDate) ?: return dates
                val endDate = parseLocalDate(task.endDate) ?: return dates
                var current = maxOf(startDate, monthStart)
                val end = minOf(endDate, monthEnd)
                while (current <= end) {
                    dates.add(current)
                    current = current.plusDays(1)
                }
            }
            // No date mode: show on all days? Or just hide from calendar.
        }

        return dates
    }

    private fun parseLocalDate(dateStr: String): LocalDate? {
        if (dateStr.isBlank()) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }
}

private fun <T> Result<T>.getOrDefault(default: T): T = getOrNull() ?: default