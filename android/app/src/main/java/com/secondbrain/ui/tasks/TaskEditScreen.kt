package com.secondbrain.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.LinkedEntityInfo
import com.secondbrain.ui.common.LinkPickerSheet
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.IconPickerDialog
import com.secondbrain.ui.util.TagInput
import com.secondbrain.ui.util.resolveIcon
import com.secondbrain.domain.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    taskId: String?,
    onNavigateBack: () -> Unit
) {
    val viewModel: TaskEditViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TaskEditViewModel(
                    taskRepository = AppModule.taskRepository,
                    taskId = taskId
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load all entities for the link picker
    var allLinkableEntities by remember { mutableStateOf<List<LinkedEntityInfo>>(emptyList()) }
    var isLinkPickerLoading by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(TaskEditEvent.DismissError)
        }
    }

    // Load entities when link picker opens
    LaunchedEffect(state.showLinkPicker) {
        if (state.showLinkPicker && allLinkableEntities.isEmpty()) {
            isLinkPickerLoading = true
            val result = AppModule.linkingRepository.getAllLinkableEntities()
            allLinkableEntities = result.getOrNull() ?: emptyList()
            isLinkPickerLoading = false
        }
    }

    // Show Link Picker
    if (state.showLinkPicker) {
        LinkPickerSheet(
            allEntities = allLinkableEntities,
            initiallySelectedIds = state.links.toSet(),
            isLoading = isLinkPickerLoading,
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissLinkPicker) },
            onConfirm = { selectedIds ->
                viewModel.onEvent(TaskEditEvent.SetLinks(selectedIds.toList()))
            }
        )
    }

    // Date picker dialog
    if (state.activeDatePicker != null) {
        DatePickerDialogContent(
            onDateSelected = { date -> viewModel.onEvent(TaskEditEvent.SetDate(state.activeDatePicker!!, date)) },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissDatePicker) }
        )
    }

    // Icon picker dialog
    if (state.showIconPicker) {
        IconPickerDialog(
            currentIcon = state.icon,
            onIconSelected = { icon ->
                viewModel.onEvent(TaskEditEvent.UpdateIcon(icon))
                viewModel.onEvent(TaskEditEvent.DismissIconPicker)
            },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissIconPicker) }
        )
    }

    // Edit mode dialog: show when editing an occurrence
    if (state.showEditModeDialog) {
        OccurrenceEditModeDialog(
            taskTitle = state.title,
            onEditThisOnly = { viewModel.onEvent(TaskEditEvent.SetOccurrenceEdit(true)) },
            onEditTemplate = { viewModel.onEvent(TaskEditEvent.SetOccurrenceEdit(false)) },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissEditModeDialog) }
        )
    }

    // Time picker dialog
    if (state.activeTimePicker != null) {
        val initialTime = when (state.activeTimePicker!!) {
            TimePickerType.Due -> state.dueTime
            TimePickerType.Start -> state.startTime
            TimePickerType.End -> state.endTime
        }
        TimePickerDialogContent(
            initialTime = initialTime,
            onTimeSelected = { time -> viewModel.onEvent(TaskEditEvent.SetTime(state.activeTimePicker!!, time)) },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissTimePicker) }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (taskId != null) "Edit Task" else "New Task",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to tasks")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(TaskEditEvent.Save) },
                        enabled = state.title.isNotBlank() && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save task")
                        }
                    }
                },
                colors = transparentTopAppBarColors()
            )
        }
    ) { padding ->
        if (state.isLoading && taskId != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Task Core Details
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Task Info",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.title,
                                onValueChange = { viewModel.onEvent(TaskEditEvent.UpdateTitle(it)) },
                                label = { Text("Title") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { viewModel.onEvent(TaskEditEvent.ShowIconPicker) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = resolveIcon(state.icon)
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Task icon",
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "T",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = state.location,
                            onValueChange = { viewModel.onEvent(TaskEditEvent.UpdateLocation(it)) },
                            label = { Text("Location") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        // -- Status --
                        Text("Status", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(
                                label = "Pending",
                                value = "pending",
                                selectedValue = state.status,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdateStatus("pending")) }
                            )
                            StatusChip(
                                label = "In Progress",
                                value = "in-progress",
                                selectedValue = state.status,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdateStatus("in-progress")) }
                            )
                            StatusChip(
                                label = "Completed",
                                value = "completed",
                                selectedValue = state.status,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdateStatus("completed")) }
                            )
                        }

                        // -- Priority --
                        Text("Priority", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriorityChip(
                                label = "None",
                                value = "",
                                selectedValue = state.priority,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdatePriority("")) }
                            )
                            PriorityChip(
                                label = "Low",
                                value = "low",
                                selectedValue = state.priority,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdatePriority("low")) }
                            )
                            PriorityChip(
                                label = "Medium",
                                value = "medium",
                                selectedValue = state.priority,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdatePriority("medium")) }
                            )
                            PriorityChip(
                                label = "High",
                                value = "high",
                                selectedValue = state.priority,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdatePriority("high")) }
                            )
                            PriorityChip(
                                label = "Urgent",
                                value = "urgent",
                                selectedValue = state.priority,
                                onClick = { viewModel.onEvent(TaskEditEvent.UpdatePriority("urgent")) }
                            )
                        }
                    }
                }

                // Section 2: Dates & Recurrence (recurrence hidden for occurrences)
                val showRecurrence = state.isTemplate || (taskId == null && state.dateMode == "range")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Dates & Recurrence",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // -- Date Mode --
                        Text("Date", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.dateMode == "",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetDateMode("")) },
                                label = { Text("None") }
                            )
                            FilterChip(
                                selected = state.dateMode == "due_date",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetDateMode("due_date")) },
                                label = { Text("Due Date") }
                            )
                            FilterChip(
                                selected = state.dateMode == "range",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetDateMode("range")) },
                                label = { Text("Date Range") }
                            )
                        }

                        // Conditional date inputs
                        if (state.dateMode == "due_date") {
                            DateField(
                                label = "Due Date",
                                date = state.dueDate,
                                onClick = { viewModel.onEvent(TaskEditEvent.ShowDatePicker(DatePickerType.Due)) },
                                onClear = { viewModel.onEvent(TaskEditEvent.SetDate(DatePickerType.Due, "")) }
                            )
                        }

                        if (state.dateMode == "range") {
                            DateField(
                                label = "Start Date",
                                date = state.startDate,
                                onClick = { viewModel.onEvent(TaskEditEvent.ShowDatePicker(DatePickerType.Start)) },
                                onClear = { viewModel.onEvent(TaskEditEvent.SetDate(DatePickerType.Start, "")) }
                            )
                            DateField(
                                label = "End Date",
                                date = state.endDate,
                                onClick = { viewModel.onEvent(TaskEditEvent.ShowDatePicker(DatePickerType.End)) },
                                onClear = { viewModel.onEvent(TaskEditEvent.SetDate(DatePickerType.End, "")) }
                            )

                            // Recurrence (only for templates and new tasks with range mode)
                            if (showRecurrence) {
                            Text("Recurrence", style = MaterialTheme.typography.titleSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecurrenceChip("None", null, state.recurrenceType) {
                                    viewModel.onEvent(TaskEditEvent.SetRecurrenceType(null))
                                }
                                RecurrenceChip("Daily", "daily", state.recurrenceType) {
                                    viewModel.onEvent(TaskEditEvent.SetRecurrenceType("daily"))
                                }
                                RecurrenceChip("Weekly", "weekly", state.recurrenceType) {
                                    viewModel.onEvent(TaskEditEvent.SetRecurrenceType("weekly"))
                                }
                                RecurrenceChip("Monthly", "monthly", state.recurrenceType) {
                                    viewModel.onEvent(TaskEditEvent.SetRecurrenceType("monthly"))
                                }
                            }
                            if (state.recurrenceType != null && state.recurrenceType != "daily") {
                                OutlinedTextField(
                                    value = state.recurrenceInterval.toString(),
                                    onValueChange = { value ->
                                        val intVal = value.toIntOrNull()
                                        if (intVal != null && intVal > 0) {
                                            viewModel.onEvent(TaskEditEvent.SetRecurrenceInterval(intVal))
                                        }
                                    },
                                    label = {
                                        when (state.recurrenceType) {
                                            "weekly" -> Text("Every N weeks")
                                            "monthly" -> Text("Every N months")
                                            else -> Text("Every N periods")
                                        }
                                    },
                                    modifier = Modifier.width(160.dp),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                                )
                            }
                            if (state.recurrenceType == "weekly") {
                                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    dayNames.forEachIndexed { index, name ->
                                        val selected = state.recurrenceDaysOfWeek.contains(index + 1)
                                        Surface(
                                            modifier = Modifier
                                                .clickable {
                                                    val newDays = if (selected) {
                                                        state.recurrenceDaysOfWeek - (index + 1)
                                                    } else {
                                                        state.recurrenceDaysOfWeek + (index + 1)
                                                    }
                                                    viewModel.onEvent(TaskEditEvent.SetRecurrenceDays(newDays))
                                                },
                                            shape = MaterialTheme.shapes.small,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = name,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                            } // end showRecurrence
                        }
                    }
                }

                // Section 2b: Time
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Time",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // -- Time Mode --
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = state.timeMode == "",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetTimeMode("")) },
                                label = { Text("None") }
                            )
                            FilterChip(
                                selected = state.timeMode == "due_time",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetTimeMode("due_time")) },
                                label = { Text("Due Time") }
                            )
                            FilterChip(
                                selected = state.timeMode == "start_end",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetTimeMode("start_end")) },
                                label = { Text("Start/End") }
                            )
                            FilterChip(
                                selected = state.timeMode == "start_duration",
                                onClick = { viewModel.onEvent(TaskEditEvent.SetTimeMode("start_duration")) },
                                label = { Text("Start + Duration") }
                            )
                        }

                        // Conditional time inputs
                        if (state.timeMode == "due_time") {
                            OutlinedButton(
                                onClick = { viewModel.onEvent(TaskEditEvent.ShowTimePicker(TimePickerType.Due)) },
                                modifier = Modifier.width(160.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.dueTime.isNotBlank()) state.dueTime else "Select time",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (state.dueTime.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.onEvent(TaskEditEvent.SetTime(TimePickerType.Due, "")) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear time", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (state.timeMode == "start_end") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(TaskEditEvent.ShowTimePicker(TimePickerType.Start)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.startTime.isNotBlank()) state.startTime else "Start",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (state.startTime.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.onEvent(TaskEditEvent.SetTime(TimePickerType.Start, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear start time", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(TaskEditEvent.ShowTimePicker(TimePickerType.End)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.endTime.isNotBlank()) state.endTime else "End",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (state.endTime.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.onEvent(TaskEditEvent.SetTime(TimePickerType.End, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear end time", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        if (state.timeMode == "start_duration") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(TaskEditEvent.ShowTimePicker(TimePickerType.Start)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (state.startTime.isNotBlank()) state.startTime else "Start",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (state.startTime.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.onEvent(TaskEditEvent.SetTime(TimePickerType.Start, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear start time", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = state.durationMinutes,
                                    onValueChange = { viewModel.onEvent(TaskEditEvent.SetDuration(it)) },
                                    label = { Text("Duration (min)") },
                                    placeholder = { Text("30") },
                                    modifier = Modifier.width(140.dp),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium,
                                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                )
                            }
                        }
                    }
                }

                // Section 3: Tags & Subtasks
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Tags & Subtasks",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TagInput(
                            tags = state.tags,
                            onTagsChanged = { viewModel.onEvent(TaskEditEvent.SetTags(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.newSubtaskTitle,
                                onValueChange = { viewModel.onEvent(TaskEditEvent.UpdateNewSubtaskTitle(it)) },
                                placeholder = { Text("New subtask") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.onEvent(TaskEditEvent.AddSubtask) }
                                )
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(TaskEditEvent.AddSubtask) },
                                enabled = state.newSubtaskTitle.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add subtask")
                            }
                        }
                        state.subtasks.forEachIndexed { index, subtask ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.onEvent(TaskEditEvent.MoveSubtask(subtask.id, -1)) },
                                        modifier = Modifier.size(24.dp),
                                        enabled = index > 0
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDropUp,
                                            contentDescription = "Move up",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.onEvent(TaskEditEvent.MoveSubtask(subtask.id, 1)) },
                                        modifier = Modifier.size(24.dp),
                                        enabled = index < state.subtasks.lastIndex
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Move down",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Checkbox(
                                    checked = subtask.completed,
                                    onCheckedChange = { viewModel.onEvent(TaskEditEvent.ToggleSubtask(subtask.id)) }
                                )
                                Text(
                                    text = subtask.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (subtask.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                IconButton(
                                    onClick = { viewModel.onEvent(TaskEditEvent.RemoveSubtask(subtask.id)) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove subtask",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3.5: Linked Entities Section
                if (state.links.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Linked Entities",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            state.links.forEach { linkId ->
                                val entityInfo = allLinkableEntities.find { it.id == linkId }
                                if (entityInfo != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (chipIcon, color) = when (entityInfo.type) {
                                            "note" -> Icons.Default.NoteAlt to MaterialTheme.colorScheme.tertiary
                                            "task" -> Icons.Default.TaskAlt to MaterialTheme.colorScheme.primary
                                            else -> Icons.Default.People to MaterialTheme.colorScheme.secondary
                                        }
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = color.copy(alpha = 0.12f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = chipIcon,
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = entityInfo.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                // Add Link Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onEvent(TaskEditEvent.ShowLinkPicker) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (state.links.isEmpty()) "Add Links" else "Edit Links (${state.links.size})",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Section 4: Description Body
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = state.body,
                            onValueChange = { viewModel.onEvent(TaskEditEvent.UpdateBody(it)) },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Checkbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.material3.Checkbox(
        checked = checked,
        onCheckedChange = { onCheckedChange(it) }
    )
}

@Composable
private fun DateField(
    label: String,
    date: String,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (date.isNotBlank()) date else "Select $label",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (date.isNotBlank()) {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Clear date", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RecurrenceChip(
    label: String,
    value: String?,
    selectedValue: String?,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selectedValue == value,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) }
    )
}

@Composable
private fun StatusChip(
    label: String,
    value: String,
    selectedValue: String,
    onClick: () -> Unit
) {
    val chipColors = when (value) {
        "pending" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.tertiary
        )
        "in-progress" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
        "completed" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.secondary
        )
        else -> FilterChipDefaults.filterChipColors()
    }
    FilterChip(
        selected = selectedValue == value,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        colors = chipColors
    )
}

@Composable
private fun PriorityChip(
    label: String,
    value: String,
    selectedValue: String,
    onClick: () -> Unit
) {
    val chipColors = when (value) {
        "low" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.tertiary
        )
        "medium" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.secondary
        )
        "high" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.error
        )
        "urgent" -> FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            selectedLabelColor = MaterialTheme.colorScheme.error
        )
        else -> FilterChipDefaults.filterChipColors()
    }
    FilterChip(
        selected = selectedValue == value,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        colors = chipColors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogContent(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = dateFormat.format(Date(millis))
                        onDateSelected(formatted)
                    } ?: onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun OccurrenceEditModeDialog(
    taskTitle: String,
    onEditThisOnly: () -> Unit,
    onEditTemplate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit occurrence") },
        text = {
            Column {
                Text(
                    text = "\"$taskTitle\" is a recurring task occurrence.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "How would you like to edit?",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEditThisOnly) {
                Text("Edit this occurrence only")
            }
        },
        dismissButton = {
            TextButton(onClick = onEditTemplate) {
                Text("Edit template (all occurrences)")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse initial time or default to current time
    val initialHour = initialTime.split(":").getOrNull(0)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val initialMinute = initialTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                TimePicker(
                    state = timePickerState
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val formatted = "%02d:%02d".format(hour, minute)
                    onTimeSelected(formatted)
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
