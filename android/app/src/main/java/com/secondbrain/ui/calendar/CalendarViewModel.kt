package com.secondbrain.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.repository.HabitRepository
import com.secondbrain.data.repository.TaskRepository
import com.secondbrain.domain.model.Habit
import com.secondbrain.domain.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DateTimeException
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val selectedDateTasks: List<Task> = emptyList(),
    val habitsForSelectedDate: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    // Filters
    val statusFilter: String = "",
    val priorityFilter: String = "",
    // Today tracking
    val today: LocalDate = LocalDate.now()
)

class CalendarViewModel(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    // Cache of loaded months to avoid redundant API calls
    private val monthCache = mutableMapOf<YearMonth, Map<LocalDate, List<Task>>>()

    // Cache of all non-template tasks to avoid repeated full fetches
    private var cachedAllTasks: List<Task>? = null

    // Cache of all habits
    private var cachedHabits: List<Habit>? = null

    init {
        loadInitialData()
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        updateSelectedDateTasks(date)
        loadHabitsForDate(date)
    }

    fun goToToday() {
        val today = LocalDate.now()
        val todayMonth = YearMonth.from(today)
        _state.update { it.copy(selectedDate = today, currentMonth = todayMonth, today = today) }
        ensureMonthLoaded(todayMonth)
        updateSelectedDateTasks(today)
        loadHabitsForDate(today)
    }

    fun setMonth(month: YearMonth) {
        _state.update { it.copy(currentMonth = month) }
        ensureMonthLoaded(month)
    }

    fun setStatusFilter(status: String) {
        _state.update { it.copy(statusFilter = status) }
        applyFilters()
    }

    fun setPriorityFilter(priority: String) {
        _state.update { it.copy(priorityFilter = priority) }
        applyFilters()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            // Invalidate caches to force fresh data
            cachedAllTasks = null
            monthCache.clear()
            loadMonthData(_state.value.currentMonth)
            updateSelectedDateTasks(_state.value.selectedDate)
            loadHabitsForDate(_state.value.selectedDate)
            // Preload adjacent months
            preloadAdjacentMonths(_state.value.currentMonth)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val currentMonth = _state.value.currentMonth
            loadMonthData(currentMonth)
            updateSelectedDateTasks(_state.value.selectedDate)
            loadHabitsForDate(_state.value.selectedDate)
            // Preload adjacent months for smooth swiping
            preloadAdjacentMonths(currentMonth)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun ensureMonthLoaded(month: YearMonth) {
        if (month in monthCache) {
            // Use cached data
            val cached = monthCache[month] ?: emptyMap()
            val filtered = applyFiltersToTaskMap(cached)
            _state.update {
                it.copy(tasksByDate = filtered)
            }
            updateSelectedDateTasks(_state.value.selectedDate)
            return
        }
        viewModelScope.launch {
            loadMonthData(month)
        }
    }

    private suspend fun loadMonthData(month: YearMonth) {
        // Fetch all tasks if not cached
        val allTasks = if (cachedAllTasks != null) {
            cachedAllTasks!!
        } else {
            val result = taskRepository.getAll()
            if (result.isFailure) {
                _state.update { it.copy(error = result.exceptionOrNull()?.message) }
                return
            }
            val tasks = result.getOrDefault(emptyList())
            cachedAllTasks = tasks
            tasks
        }

        val nonTemplateTasks = allTasks.filter { !it.isTemplate }
        val tasksByDate = groupTasksByMonth(nonTemplateTasks, month)

        // Cache the result
        monthCache[month] = tasksByDate

        // Apply current filters
        val filtered = applyFiltersToTaskMap(tasksByDate)

        _state.update {
            it.copy(
                tasksByDate = filtered,
                error = null
            )
        }
        updateSelectedDateTasks(_state.value.selectedDate)
    }

    private suspend fun preloadAdjacentMonths(month: YearMonth) {
        val allTasks = cachedAllTasks ?: return
        val nonTemplateTasks = allTasks.filter { !it.isTemplate }
        for (offset in -1..1 step 2) {
            val adjMonth = month.plusMonths(offset.toLong())
            if (adjMonth !in monthCache) {
                val adjTasks = groupTasksByMonth(nonTemplateTasks, adjMonth)
                monthCache[adjMonth] = adjTasks
            }
        }
    }

    private fun groupTasksByMonth(tasks: List<Task>, month: YearMonth): Map<LocalDate, List<Task>> {
        val startOfMonth = month.atDay(1)
        val endOfMonth = month.atEndOfMonth()
        val result = mutableMapOf<LocalDate, MutableList<Task>>()

        for (task in tasks) {
            val taskDates = getTaskDates(task, startOfMonth, endOfMonth)
            for (date in taskDates) {
                result.getOrPut(date) { mutableListOf() }.add(task)
            }
        }
        return result
    }

    private fun updateSelectedDateTasks(date: LocalDate) {
        val cachedMonth = monthCache[YearMonth.from(date)]
        val tasks = cachedMonth?.get(date) ?: emptyList()
        val filtered = applyFilters(tasks)
        _state.update { it.copy(selectedDateTasks = filtered) }
    }

    private fun loadHabitsForDate(date: LocalDate) {
        viewModelScope.launch {
            if (cachedHabits == null) {
                val result = habitRepository.getAll()
                if (result.isSuccess) {
                    cachedHabits = result.getOrDefault(emptyList())
                }
            }
            val allHabits = cachedHabits ?: emptyList()
            // Show habits scheduled for this weekday
            val dayOfWeek = date.dayOfWeek.value % 7 // 0=Mon..6=Sun
            val isoDayOfWeek = if (dayOfWeek == 0) 7 else dayOfWeek
            val scheduledHabits = allHabits.filter { habit ->
                habit.daysOfWeek.isEmpty() || isoDayOfWeek in habit.daysOfWeek
            }
            _state.update { it.copy(habitsForSelectedDate = scheduledHabits) }
        }
    }

    private fun applyFilters(): Map<LocalDate, List<Task>> {
        val currentMonth = _state.value.currentMonth
        val monthData = monthCache[currentMonth] ?: return emptyMap()
        val filtered = applyFiltersToTaskMap(monthData)
        _state.update { it.copy(tasksByDate = filtered) }
        updateSelectedDateTasks(_state.value.selectedDate)
        return filtered
    }

    private fun applyFiltersToTaskMap(taskMap: Map<LocalDate, List<Task>>): Map<LocalDate, List<Task>> {
        val statusFilter = _state.value.statusFilter
        val priorityFilter = _state.value.priorityFilter
        if (statusFilter.isEmpty() && priorityFilter.isEmpty()) return taskMap

        return taskMap.mapValues { (_, tasks) ->
            applyFilters(tasks)
        }
    }

    private fun applyFilters(tasks: List<Task>): List<Task> {
        var filtered = tasks
        val statusFilter = _state.value.statusFilter
        val priorityFilter = _state.value.priorityFilter
        if (statusFilter.isNotEmpty()) {
            filtered = filtered.filter { it.displayStatus == statusFilter }
        }
        if (priorityFilter.isNotEmpty()) {
            filtered = filtered.filter { it.priority == priorityFilter }
        }
        return filtered
    }

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
        }

        return dates
    }

    private fun parseLocalDate(dateStr: String): LocalDate? {
        if (dateStr.isBlank()) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeException) {
            null
        }
    }
}

private fun <T> Result<T>.getOrDefault(default: T): T = getOrNull() ?: default