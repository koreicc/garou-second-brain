package com.secondbrain.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.LinkedEntityInfo
import com.secondbrain.ui.common.AnimatedSection
import com.secondbrain.ui.common.LinkPickerSheet
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.IconPickerDialog
import com.secondbrain.ui.util.TagInput
import com.secondbrain.ui.util.resolveIcon
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HabitEditScreen(
    habitId: String?,
    onNavigateBack: () -> Unit
) {
    val viewModel: HabitEditViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HabitEditViewModel(
                    habitRepository = AppModule.habitRepository,
                    habitId = habitId
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

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.onEvent(HabitEditEvent.DismissError)
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
            onDismiss = { viewModel.onEvent(HabitEditEvent.DismissLinkPicker) },
            onConfirm = { selectedIds ->
                viewModel.onEvent(HabitEditEvent.SetLinks(selectedIds.toList()))
            }
        )
    }

    // Icon picker dialog
    if (state.showIconPicker) {
        IconPickerDialog(
            currentIcon = state.icon,
            onIconSelected = { icon ->
                viewModel.onEvent(HabitEditEvent.UpdateIcon(icon))
                viewModel.onEvent(HabitEditEvent.DismissIconPicker)
            },
            onDismiss = { viewModel.onEvent(HabitEditEvent.DismissIconPicker) }
        )
    }

    // Time picker dialog
    if (state.activeTimePicker != null) {
        val initialTime = when (state.activeTimePicker!!) {
            HabitTimePickerType.Due -> state.dueTime
            HabitTimePickerType.Start -> state.startTime
            HabitTimePickerType.End -> state.endTime
        }
        HabitTimePickerDialogContent(
            initialTime = initialTime,
            onTimeSelected = { time -> viewModel.onEvent(HabitEditEvent.SetTime(state.activeTimePicker!!, time)) },
            onDismiss = { viewModel.onEvent(HabitEditEvent.DismissTimePicker) }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (habitId != null) "Edit Habit" else "New Habit",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back to habits")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(HabitEditEvent.Save) },
                        enabled = state.title.isNotBlank() && state.daysOfWeek.isNotEmpty() && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = "Save habit")
                        }
                    }
                },
                colors = transparentTopAppBarColors()
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.onEvent(HabitEditEvent.Save) },
                        modifier = Modifier.weight(1f),
                        enabled = state.title.isNotBlank() && state.daysOfWeek.isNotEmpty() && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (state.isLoading && habitId != null) {
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
                // Section 1: Habit Core Details
                AnimatedSection(index = 0) {
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
                            text = "Habit Info",
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
                                onValueChange = { viewModel.onEvent(HabitEditEvent.UpdateTitle(it)) },
                                label = { Text("Title") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium
                            )
                            Surface(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clickable { viewModel.onEvent(HabitEditEvent.ShowIconPicker) },
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val icon = resolveIcon(state.icon)
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = "Habit icon",
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "H",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // -- Priority --
                        Text("Priority", style = MaterialTheme.typography.titleSmall)
                        var prioritySheetVisible by remember { mutableStateOf(false) }
                        val priorityLabel = when (state.priority) {
                            "low" -> "Low"
                            "medium" -> "Medium"
                            "high" -> "High"
                            "urgent" -> "Urgent"
                            else -> "None"
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prioritySheetVisible = true },
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "\u2691 $priorityLabel",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        if (prioritySheetVisible) {
                            ModalBottomSheet(
                                onDismissRequest = { prioritySheetVisible = false },
                                sheetState = rememberModalBottomSheetState()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Priority", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    listOf("" to "None", "low" to "Low", "medium" to "Medium", "high" to "High", "urgent" to "Urgent").forEach { (value, label) ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.onEvent(HabitEditEvent.UpdatePriority(value))
                                                    prioritySheetVisible = false
                                                },
                                            color = if (state.priority == value) MaterialTheme.colorScheme.primaryContainer
                                                   else Color.Transparent
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(16.dp),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                    }
                }
            }

            // Section 2: Days of Week
            AnimatedSection(index = 1) {
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
                            text = "Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text("Days of Week", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            dayLabels.forEachIndexed { index, label ->
                                val day = index + 1
                                val isSelected = day in state.daysOfWeek
                                Surface(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clickable { viewModel.onEvent(HabitEditEvent.ToggleDay(day)) },
                                    shape = MaterialTheme.shapes.small,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = label,
                                            color = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // -- Time Mode --
                        Text("Time", style = MaterialTheme.typography.titleSmall)
                        val timeModeOptions = listOf("" to "None", "due_time" to "Due Time", "start_end" to "Start/End", "start_duration" to "Duration")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            timeModeOptions.forEachIndexed { i, (value, label) ->
                                SegmentedButton(
                                    selected = state.timeMode == value,
                                    onClick = { viewModel.onEvent(HabitEditEvent.SetTimeMode(value)) },
                                    shape = SegmentedButtonDefaults.itemShape(i, timeModeOptions.size),
                                    icon = { SegmentedButtonDefaults.Icon(active = state.timeMode == value) },
                                ) { Text(label, style = MaterialTheme.typography.bodySmall) }
                            }
                        }

                        // Conditional time inputs
                        AnimatedVisibility(
                            visible = state.timeMode == "due_time",
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.onEvent(HabitEditEvent.ShowTimePicker(HabitTimePickerType.Due)) },
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
                                    onClick = { viewModel.onEvent(HabitEditEvent.SetTime(HabitTimePickerType.Due, "")) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear time", modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = state.timeMode == "start_end",
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(HabitEditEvent.ShowTimePicker(HabitTimePickerType.Start)) },
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
                                        onClick = { viewModel.onEvent(HabitEditEvent.SetTime(HabitTimePickerType.Start, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear start time", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(HabitEditEvent.ShowTimePicker(HabitTimePickerType.End)) },
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
                                        onClick = { viewModel.onEvent(HabitEditEvent.SetTime(HabitTimePickerType.End, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear end time", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = state.timeMode == "start_duration",
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(HabitEditEvent.ShowTimePicker(HabitTimePickerType.Start)) },
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
                                        onClick = { viewModel.onEvent(HabitEditEvent.SetTime(HabitTimePickerType.Start, "")) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear start time", modifier = Modifier.size(18.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = state.durationMinutes,
                                    onValueChange = { viewModel.onEvent(HabitEditEvent.SetDuration(it)) },
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

            // Section 3: Subtasks
            AnimatedSection(index = 2) {
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
                            text = "Subtasks",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = state.newSubtaskTitle,
                                onValueChange = { viewModel.onEvent(HabitEditEvent.UpdateNewSubtaskTitle(it)) },
                                placeholder = { Text("New subtask") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.onEvent(HabitEditEvent.AddSubtask) }
                                )
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(HabitEditEvent.AddSubtask) },
                                enabled = state.newSubtaskTitle.isNotBlank()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add subtask")
                            }
                        }
                        if (state.subtasks.isNotEmpty()) {
                            val reorderState = rememberReorderableLazyListState(
                                onMove = { from, to ->
                                    viewModel.onEvent(HabitEditEvent.ReorderSubtasks(from.index, to.index))
                                }
                            )
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .reorderable(reorderState),
                                state = reorderState.listState,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(
                                    items = state.subtasks,
                                    key = { _, subtask -> subtask.id }
                                ) { index, subtask ->
                                    ReorderableItem(reorderState, key = subtask.id) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                modifier = Modifier
                                                    .detectReorderAfterLongPress(reorderState)
                                                    .size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            androidx.compose.material3.Checkbox(
                                                checked = subtask.completed,
                                                onCheckedChange = { viewModel.onEvent(HabitEditEvent.ToggleSubtask(subtask.id)) }
                                            )
                                            Text(
                                                text = subtask.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(
                                                onClick = { viewModel.onEvent(HabitEditEvent.RemoveSubtask(subtask.id)) },
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
                    }
                }
            }

            // Section 4: Body/description
            AnimatedSection(index = 3) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = state.body,
                            onValueChange = { viewModel.onEvent(HabitEditEvent.UpdateBody(it)) },
                            placeholder = { Text("Add notes...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            // Section 5: More details chip -> sheet
            AnimatedSection(index = 4) {
                var moreDetailsSheetVisible by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { moreDetailsSheetVisible = true },
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
                            text = "More details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (moreDetailsSheetVisible) {
                    HabitMoreDetailsSheet(
                        location = state.location,
                        onLocationChange = { viewModel.onEvent(HabitEditEvent.UpdateLocation(it)) },
                        tags = state.tags,
                        onTagsChange = { viewModel.onEvent(HabitEditEvent.SetTags(it)) },
                        links = state.links,
                        allLinkableEntities = allLinkableEntities,
                        onShowLinkPicker = { viewModel.onEvent(HabitEditEvent.ShowLinkPicker) },
                        onDismiss = { moreDetailsSheetVisible = false }
                    )
                }
            }

            // Section 6: Linked Entities (if any)
            if (state.links.isNotEmpty()) {
                AnimatedSection(index = 5) {
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
            }
        }
    }
            }
        }
    }
}

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitTimePickerDialogContent(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                TimePicker(state = timePickerState)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitMoreDetailsSheet(
    location: String,
    onLocationChange: (String) -> Unit,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    links: List<String>,
    allLinkableEntities: List<LinkedEntityInfo>,
    onShowLinkPicker: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("More details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Text("Tags", style = MaterialTheme.typography.titleSmall)
            TagInput(
                tags = tags,
                onTagsChanged = onTagsChange,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Links", style = MaterialTheme.typography.titleSmall)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowLinkPicker() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (links.isEmpty()) "Add links" else "${links.size} link(s) selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (links.isNotEmpty()) {
                links.forEach { linkId ->
                    val entityInfo = allLinkableEntities.find { it.id == linkId }
                    if (entityInfo != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
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
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = chipIcon,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entityInfo.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


