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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.IconPickerDialog
import com.secondbrain.ui.util.TagInput
import com.secondbrain.ui.util.resolveIcon
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

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Start date picker dialog
    if (state.showStartDatePicker) {
        DatePickerDialogContent(
            onDateSelected = { date -> viewModel.onEvent(TaskEditEvent.SetStartDate(date)) },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissStartDatePicker) }
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

    // End date picker dialog
    if (state.showEndDatePicker) {
        DatePickerDialogContent(
            onDateSelected = { date -> viewModel.onEvent(TaskEditEvent.SetEndDate(date)) },
            onDismiss = { viewModel.onEvent(TaskEditEvent.DismissEndDatePicker) }
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
                        enabled = state.title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save task")
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
                    }
                }

                // Section 2: Dates & Recurrence
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
                        DateField(
                            label = "Start Date",
                            date = state.startDate,
                            onClick = { viewModel.onEvent(TaskEditEvent.ShowStartDatePicker) },
                            onClear = { viewModel.onEvent(TaskEditEvent.SetStartDate("")) }
                        )

                        DateField(
                            label = "End Date",
                            date = state.endDate,
                            onClick = { viewModel.onEvent(TaskEditEvent.ShowEndDatePicker) },
                            onClear = { viewModel.onEvent(TaskEditEvent.SetEndDate("")) }
                        )

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
                                                viewModel.onEvent(TaskEditEvent.SetRecurrenceDaysOfWeek(newDays))
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
