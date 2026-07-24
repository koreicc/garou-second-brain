package com.secondbrain.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Subtask
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.PriorityBadge
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime
import com.secondbrain.ui.util.resolveIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
// ---------------------------------------------------------------------------
// Top-level screen composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPeople: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(
                    noteRepository = AppModule.noteRepository,
                    taskRepository = AppModule.taskRepository,
                    quickTaskRepository = AppModule.quickTaskRepository,
                    personRepository = AppModule.personRepository
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
    }
    RefreshOnResume {
        viewModel.silentReload()
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = java.time.ZoneId.systemDefault()
                .let { zone ->
                    state.selectedDate.atStartOfDay(zone)
                        .toInstant()
                        .toEpochMilli()
                }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val ld = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                            viewModel.onEvent(DashboardEvent.SelectDate(ld))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
        ) {
            DatePicker(state = datePickerState)
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                            text = state.dateString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                },
                colors = transparentTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = { onNavigateToSearch() }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search"
                        onClick = { onNavigateToCalendar() }
                            Icons.Default.DateRange,
                            contentDescription = "Calendar view"
                        onClick = { viewModel.onEvent(DashboardEvent.LoadData) },
                        enabled = !state.isLoading
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh dashboard")
            )
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
            // ---- Scope filter chips ----
            item(key = "scope-chips") {
                ScopeChipsRow(
                    selectedScope = state.selectedScope,
                    onSelectScope = { scope -> viewModel.onEvent(DashboardEvent.SelectScope(scope)) },
                    onOpenDatePicker = { showDatePicker = true }
                )
            // ---- Date selector (hidden in week view) ----
            if (state.selectedScope != "week") {
                item(key = "date-selector") {
                    DateSelectorCard(
                        selectedDate = state.selectedDate,
                        onChangeDate = { date -> viewModel.onEvent(DashboardEvent.SelectDate(date)) },
                        onOpenDatePicker = { showDatePicker = true }
                    )
            // ---- Routine section ----
            state.routine?.let { routine ->
                item(key = "routine") {
                    RoutineSection(
                        routine = routine,
                        timeOfDay = state.routineTimeOfDay,
                        onToggleSubtask = { subtaskId ->
                            viewModel.onEvent(DashboardEvent.ToggleRoutineSubtask(subtaskId))
                        },
                        onCompleteRoutine = {
                            viewModel.onEvent(DashboardEvent.CompleteRoutine)
            // ---- Overdue tasks ----
            if (state.overdueTasks.isNotEmpty()) {
                item(key = "overdue-tasks") {
                    OverdueTasksSection(
                        tasks = state.overdueTasks,
                        onTaskClick = onNavigateToTaskDetail
            // ---- Tasks section: week view or single day ----
            if (state.selectedScope == "week") {
                item(key = "week-tasks") {
                    WeekTasksSection(
                        weekStartDate = state.weekStartDate,
                        tasksByDay = state.weekTasksByDay,
            } else {
                item(key = "date-tasks") {
                    DateTasksSection(
                        date = state.selectedDate,
                        tasks = state.selectedDateTasks,
            // ---- Quick task input ----
            item(key = "quick-task-input") {
                QuickTaskInputCard(
                    onAddQuickTask = { title ->
                        viewModel.onEvent(DashboardEvent.CreateQuickTask(title = title))
            // ---- Quick task list ----
            items(state.quickTasks, key = { it.id }) { qTask ->
                val countdown = state.completingQuickTasks[qTask.id]
                QuickTaskRowCard(
                    quickTask = qTask,
                    countdown = countdown,
                    onComplete = { viewModel.onEvent(DashboardEvent.CompleteQuickTask(qTask.id)) },
                    onDelete = { viewModel.onEvent(DashboardEvent.DeleteQuickTask(qTask.id)) }
            // ---- Quick note input ----
            item(key = "quick-note-input") {
                QuickNoteInputCard(
                    title = state.quickNoteTitle,
                    content = state.quickNoteContent,
                    onTitleChange = { viewModel.onEvent(DashboardEvent.UpdateQuickNoteTitle(it)) },
                    onContentChange = { viewModel.onEvent(DashboardEvent.UpdateQuickNoteContent(it)) },
                    onAddNote = { viewModel.onEvent(DashboardEvent.CreateQuickNote) }
            // ---- Loading indicator ----
            if (state.isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                        CircularProgressIndicator()
            // ---- Bottom spacer ----
            item(key = "bottom-spacer") {
                Spacer(modifier = Modifier.height(32.dp))
}
// Scope filter chips row
private fun ScopeChipsRow(
    selectedScope: String,
    onSelectScope: (String) -> Unit,
    onOpenDatePicker: () -> Unit
    val scopes = listOf(
        "today" to "Today",
        "tomorrow" to "Tomorrow",
        "week" to "This Week",
        "date" to "Pick Date"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        scopes.forEach { (value, label) ->
            val isSelected = selectedScope == value
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (value == "date") {
                        onOpenDatePicker()
                    } else {
                        onSelectScope(value)
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
// Week tasks section
private fun WeekTasksSection(
    weekStartDate: LocalDate,
    tasksByDay: Map<LocalDate, List<Task>>,
    onTaskClick: (String) -> Unit
    val weekEndDate = weekStartDate.plusDays(6)
    val headerText = "This Week: ${weekStartDate.format(DateTimeFormatter.ofPattern("MMM d"))} - ${weekEndDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            Spacer(modifier = Modifier.height(12.dp))
            var current = weekStartDate
            val today = LocalDate.now()
            while (current <= weekEndDate) {
                val dayTasks = tasksByDay[current] ?: emptyList()
                val isToday = current == today
                val dayLabel = current.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                val dayColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                Text(
                    text = if (isToday) "$dayLabel (Today)" else dayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                    color = dayColor,
                    modifier = Modifier.padding(vertical = 4.dp)
                if (dayTasks.isEmpty()) {
                    Text(
                        text = "No tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                } else {
                    dayTasks.forEach { task ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, bottom = 4.dp)
                                .clickable { onTaskClick(task.id) },
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconVector = resolveIcon(task.icon)
                                if (iconVector != null) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = task.icon,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else if (task.icon.isNotEmpty()) {
                                    Text(
                                        text = task.icon,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                }
                                if (task.icon.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PriorityBadge(priority = task.priority)
                                    StatusBadge(status = task.displayStatus)
                            }
                current = current.plusDays(1)
                if (current <= weekEndDate) {
                    Spacer(modifier = Modifier.height(4.dp))
// Overdue tasks section
private fun OverdueTasksSection(
    tasks: List<Task>,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                    text = "Overdue (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
            Spacer(modifier = Modifier.height(8.dp))
            tasks.forEach { task ->
                DateTaskItem(
                    task = task,
                    onClick = { onTaskClick(task.id) }
                if (task != tasks.last()) {
                    Spacer(modifier = Modifier.height(6.dp))
// Date selector card
private fun DateSelectorCard(
    selectedDate: LocalDate,
    onChangeDate: (LocalDate) -> Unit,
        Row(
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
            IconButton(onClick = { onChangeDate(selectedDate.minusDays(1)) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDatePicker),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
                    Icons.Default.CalendarToday,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    val today = LocalDate.now()
                    val label = when {
                        selectedDate == today -> "Today"
                        selectedDate == today.minusDays(1) -> "Yesterday"
                        selectedDate == today.plusDays(1) -> "Tomorrow"
                        else -> ""
                    if (label.isNotEmpty()) {
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
            IconButton(onClick = { onChangeDate(selectedDate.plusDays(1)) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
// Selected date tasks section
private fun DateTasksSection(
    date: LocalDate,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                    text = "Tasks for this day (${tasks.size})",
                    color = MaterialTheme.colorScheme.onSurface
            if (tasks.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                    text = "No tasks for this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                Spacer(modifier = Modifier.height(8.dp))
                tasks.forEach { task ->
                    DateTaskItem(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    if (task != tasks.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
private fun DateTaskItem(
    task: Task,
    onClick: () -> Unit
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
            val iconVector = resolveIcon(task.icon)
            if (iconVector != null) {
                    imageVector = iconVector,
                    contentDescription = task.icon,
            } else if (task.icon.isNotEmpty()) {
                    text = task.icon,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
            if (task.icon.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                if (task.parentId.isNotEmpty()) {
                        text = "Occurrence",
                        color = MaterialTheme.colorScheme.tertiary
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PriorityBadge(priority = task.priority)
                StatusBadge(status = task.displayStatus)
// Routine section
private fun RoutineSection(
    routine: RoutineInfo,
    timeOfDay: String,
    onToggleSubtask: (String) -> Unit,
    onCompleteRoutine: () -> Unit
    val label = when (timeOfDay) {
        "morning" -> "Morning Routine"
        "evening" -> "Evening Routine"
        else -> "Routine"
                    text = label,
                if (routine.totalCount > 0) {
                        text = "${routine.completedCount}/${routine.totalCount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (routine.isComplete)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
            if (routine.totalCount > 0) {
                LinearProgressIndicator(
                    progress = { routine.completedCount.toFloat() / routine.totalCount.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
            routine.task.subtasks.forEach { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    onToggle = { onToggleSubtask(subtask.id) }
            if (!routine.isComplete && routine.totalCount > 0) {
                FilledTonalButton(
                    onClick = onCompleteRoutine,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Routine")
private fun SubtaskRow(
    subtask: Subtask,
    onToggle: () -> Unit
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
        Checkbox(
            checked = subtask.completed,
            onCheckedChange = { onToggle() }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (subtask.completed)
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
// Quick Task input card
private fun QuickTaskInputCard(onAddQuickTask: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                    Icons.Default.FlashOn,
                    tint = MaterialTheme.colorScheme.primary,
                    "Quick Task",
                    fontWeight = FontWeight.SemiBold
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                FilledTonalIconButton(
                        if (title.isNotBlank()) {
                            onAddQuickTask(title.trim())
                            title = ""
                    },
                    enabled = title.isNotBlank(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    Icon(Icons.Default.Add, contentDescription = "Add quick task")
// Quick task row card
private fun QuickTaskRowCard(
    quickTask: QuickTask,
    countdown: Int?,
    onComplete: () -> Unit,
    onDelete: () -> Unit
                .padding(horizontal = 16.dp, vertical = 14.dp),
                    text = quickTask.title,
                if (quickTask.createdAt.isNotEmpty() && countdown == null) {
                        text = formatRelativeTime(quickTask.createdAt),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                if (countdown != null) {
                        text = "Completed. Deleting in $countdown...",
                        color = MaterialTheme.colorScheme.secondary
            if (countdown == null) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onComplete() }
                    text = countdown.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(40.dp)
            IconButton(onClick = onDelete) {
                    Icons.Default.Delete,
                    contentDescription = "Delete quick task: ${quickTask.title}",
                    tint = MaterialTheme.colorScheme.error
// Quick Note input card
private fun QuickNoteInputCard(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onAddNote: () -> Unit
                    Icons.Default.NoteAlt,
                    "Quick Note",
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
                    onValueChange = onTitleChange,
                    placeholder = { Text("Title") },
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = { Text("What's on your mind?") },
                    minLines = 3,
                    maxLines = 5
                Row(
                    horizontalArrangement = Arrangement.End
                    FilledTonalButton(
                        onClick = onAddNote,
                        enabled = title.isNotBlank()
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Note")