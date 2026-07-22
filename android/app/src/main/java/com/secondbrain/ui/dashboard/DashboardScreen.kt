package com.secondbrain.ui.dashboard

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime
import com.secondbrain.ui.util.resolveIcon

// ---------------------------------------------------------------------------
// Top-level screen composable -- signature must remain unchanged
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNotes: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToPeople: () -> Unit,
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
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

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    RefreshOnResume {
        viewModel.silentReload()
    }

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
                        Text(
                            text = state.dateString,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = transparentTopAppBarColors(),
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(DashboardEvent.LoadData) },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh dashboard"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        }
                    )
                }
            }

            // ---- Today's tasks section ----
            item(key = "today-tasks") {
                TodaysTasksSection(
                    tasks = state.todayTasks,
                    onTaskClick = { taskId -> onNavigateToTaskDetail(taskId) },
                    onSeeAll = onNavigateToTasks
                )
            }

            // ---- Quick task input ----
            item(key = "quick-task-input") {
                QuickTaskInputCard(
                    onAddQuickTask = { title ->
                        viewModel.onEvent(DashboardEvent.CreateQuickTask(title = title))
                    }
                )
            }

            // ---- Quick task list ----
            items(state.quickTasks, key = { it.id }) { qTask ->
                val countdown = state.completingQuickTasks[qTask.id]
                QuickTaskRowCard(
                    quickTask = qTask,
                    countdown = countdown,
                    onComplete = { viewModel.onEvent(DashboardEvent.CompleteQuickTask(qTask.id)) },
                    onDelete = { viewModel.onEvent(DashboardEvent.DeleteQuickTask(qTask.id)) }
                )
            }

            // ---- Quick note input ----
            item(key = "quick-note-input") {
                QuickNoteInputCard(
                    title = state.quickNoteTitle,
                    content = state.quickNoteContent,
                    onTitleChange = { viewModel.onEvent(DashboardEvent.UpdateQuickNoteTitle(it)) },
                    onContentChange = { viewModel.onEvent(DashboardEvent.UpdateQuickNoteContent(it)) },
                    onAddNote = { viewModel.onEvent(DashboardEvent.CreateQuickNote) }
                )
            }

            // ---- Loading indicator ----
            if (state.isLoading) {
                item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // ---- Bottom spacer ----
            item(key = "bottom-spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Routine section
// ---------------------------------------------------------------------------

@Composable
private fun RoutineSection(
    routine: RoutineInfo,
    timeOfDay: String,
    onToggleSubtask: (String) -> Unit,
    onCompleteRoutine: () -> Unit
) {
    val label = when (timeOfDay) {
        "morning" -> "Morning Routine"
        "evening" -> "Evening Routine"
        else -> "Routine"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (routine.totalCount > 0) {
                    Text(
                        text = "${routine.completedCount}/${routine.totalCount}",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (routine.isComplete)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress bar
            if (routine.totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { routine.completedCount.toFloat() / routine.totalCount.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            // Subtask list
            Spacer(modifier = Modifier.height(8.dp))
            routine.task.subtasks.forEach { subtask ->
                SubtaskRow(
                    subtask = subtask,
                    onToggle = { onToggleSubtask(subtask.id) }
                )
            }

            // Complete Routine button
            if (!routine.isComplete && routine.totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onCompleteRoutine,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Complete Routine")
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(
    subtask: Subtask,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = subtask.completed,
            onCheckedChange = { onToggle() }
        )
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
        )
    }
}

// ---------------------------------------------------------------------------
// Today's tasks section
// ---------------------------------------------------------------------------

@Composable
private fun TodaysTasksSection(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Tasks (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (tasks.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No tasks scheduled for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                tasks.forEach { task ->
                    TodaysTaskItem(
                        task = task,
                        onClick = { onTaskClick(task.id) }
                    )
                    if (task != tasks.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // "See all" link
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(
                onClick = onSeeAll,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("See all")
            }
        }
    }
}

@Composable
private fun TodaysTaskItem(
    task: Task,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconVector = resolveIcon(task.icon)
            if (iconVector != null) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = task.icon,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (task.icon.isNotEmpty()) {
                Text(
                    text = task.icon,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (task.icon.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Status badge
            StatusBadge(status = task.status)
        }
    }
}

// ---------------------------------------------------------------------------
// Quick Task input card
// ---------------------------------------------------------------------------

@Composable
private fun QuickTaskInputCard(onAddQuickTask: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Quick Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalIconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddQuickTask(title.trim())
                            title = ""
                        }
                    },
                    enabled = title.isNotBlank(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add quick task")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Quick task row card
// ---------------------------------------------------------------------------

@Composable
private fun QuickTaskRowCard(
    quickTask: QuickTask,
    countdown: Int?,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quickTask.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (quickTask.createdAt.isNotEmpty() && countdown == null) {
                    Text(
                        text = formatRelativeTime(quickTask.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (countdown != null) {
                    Text(
                        text = "Completed. Deleting in $countdown...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (countdown == null) {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onComplete() }
                )
            } else {
                Text(
                    text = countdown.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete quick task: ${quickTask.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Quick Note input card
// ---------------------------------------------------------------------------

@Composable
private fun QuickNoteInputCard(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onAddNote: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.NoteAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Quick Note",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    placeholder = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = onAddNote,
                        enabled = title.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Note")
                    }
                }
            }
        }
    }
}
