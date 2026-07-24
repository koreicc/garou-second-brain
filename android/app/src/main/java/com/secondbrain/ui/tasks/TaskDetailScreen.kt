package com.secondbrain.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Subtask
import com.secondbrain.ui.theme.pillShape
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.WikilinkText
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.resolveIcon
import com.secondbrain.ui.common.LinkedEntitiesView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (String) -> Unit = {},
    onNavigateToTask: (String) -> Unit = {},
    onNavigateToPerson: (String) -> Unit = {}
) {
    val viewModel: TaskDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TaskDetailViewModel(
                    taskRepository = AppModule.taskRepository,
                    searchRepository = AppModule.searchRepository,
                    taskId = taskId
                ) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Navigate when a wikilink is resolved
    LaunchedEffect(state.wikilinkNavigationTarget) {
        state.wikilinkNavigationTarget?.let { target ->
            viewModel.clearWikilinkNavigation()
            when (target.type) {
                "note" -> onNavigateToNote(target.id)
                "task" -> onNavigateToTask(target.id)
                "person" -> onNavigateToPerson(target.id)
            }
        }
    }

    // Reload task data when navigating back from edit screen
    RefreshOnResume {
        viewModel.reload()
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = transparentTopAppBarColors(),
                title = {
                    Text(
                        text = state.task?.title ?: "Task",
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
                    if (state.task?.status != "completed") {
                        IconButton(onClick = { viewModel.onEvent(TaskDetailEvent.Complete) }) {
                            Icon(Icons.Default.Check, contentDescription = "Complete task")
                        }
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit task")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.task != null -> {
                val task = state.task!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val iconVector = resolveIcon(task.icon)
                        if (iconVector != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = task.icon,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        StatusBadge(status = task.status)
                    }

                    if (task.location.isNotEmpty() || task.dateMode.isNotEmpty() || task.timeMode.isNotEmpty() || task.tags.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (task.location.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(task.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                // Date display
                                if (task.dateMode.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val dateText = when (task.dateMode) {
                                            "due_date" -> "Due: ${task.dueDate.take(10)}"
                                            "range" -> {
                                                val startStr = task.startDate.take(10)
                                                val endStr = task.endDate.take(10)
                                                if (startStr == endStr || endStr.isEmpty()) startStr else "$startStr - $endStr"
                                            }
                                            else -> ""
                                        }
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Time display
                                if (task.timeMode.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        val timeText = when (task.timeMode) {
                                            "due_time" -> "Due: ${task.dueTime}"
                                            "start_end" -> "${task.startTime} - ${task.endTime}"
                                            "start_duration" -> "${task.startTime} + ${task.durationMinutes}min"
                                            else -> ""
                                        }
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (task.tags.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.Tag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp).padding(top = 6.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            task.tags.forEach { tag ->
                                                SuggestionChip(
                                                    onClick = { },
                                                    label = { Text("#$tag") },
                                                    shape = pillShape,
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (task.links.isNotEmpty()) {
                        LinkedEntitiesView(
                            linkIds = task.links,
                            onNavigateToNote = onNavigateToNote,
                            onNavigateToTask = onNavigateToTask,
                            onNavigateToPerson = onNavigateToPerson
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (task.subtasks.isNotEmpty()) {
                        val completedCount = task.subtasks.count { it.completed }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                                Text(
                                    text = "Subtasks ($completedCount/${task.subtasks.size} completed)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                task.subtasks.forEachIndexed { index, subtask ->
                                    SubtaskRow(
                                        subtask = subtask,
                                        onCheckedChange = { viewModel.onEvent(TaskDetailEvent.ToggleSubtask(subtask.id)) }
                                    )
                                    if (index < task.subtasks.lastIndex) {
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (task.body.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                WikilinkText(
                                    text = task.body,
                                    onWikilinkClick = { target ->
                                        viewModel.onEvent(TaskDetailEvent.ResolveWikilink(target))
                                    },
                                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(subtask: Subtask, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Checkbox(
            checked = subtask.completed,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else TextDecoration.None,
            color = if (subtask.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
