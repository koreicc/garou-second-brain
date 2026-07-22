package com.secondbrain.ui.dashboard

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
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime

@Composable
private fun QuickTaskRow(
    quickTask: QuickTask,
    countdown: Int?,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

    // Show errors in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    // Reload data silently every time the screen resumes (no loading flicker)
    RefreshOnResume {
        viewModel.silentReload()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Dashboard",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                QuickTaskCard(
                    onAddQuickTask = { title ->
                        viewModel.onEvent(DashboardEvent.CreateQuickTask(title = title))
                    }
                )
            }

            item {
                StatsRow(
                    noteCount = state.noteCount,
                    taskCount = state.taskCount,
                    quickTaskCount = state.quickTasks.size,
                    personCount = state.personCount
                )
            }

            if (state.quickTasks.isNotEmpty()) {
                item {
                    Text(
                        "Quick Tasks (${state.quickTasks.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(state.quickTasks, key = { it.id }) { qt ->
                    val countdown = state.completingQuickTasks[qt.id]
                    QuickTaskRow(
                        quickTask = qt,
                        countdown = countdown,
                        onComplete = { viewModel.onEvent(DashboardEvent.CompleteQuickTask(qt.id)) },
                        onDelete = { viewModel.onEvent(DashboardEvent.DeleteQuickTask(qt.id)) }
                    )
                }
            }

            if (state.recentNotes.isNotEmpty()) {
                item {
                    Text("Recent Notes", style = MaterialTheme.typography.titleMedium)
                }
                items(state.recentNotes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onNavigateToNoteDetail(note.id) })
                }
            }

            if (state.recentTasks.isNotEmpty()) {
                item {
                    Text("Active Tasks", style = MaterialTheme.typography.titleMedium)
                }
                items(state.recentTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onNavigateToTaskDetail(task.id) })
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTaskCard(onAddQuickTask: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Task", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
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
                IconButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddQuickTask(title.trim())
                            title = ""
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add quick task")
                }
            }
        }
    }
}

@Composable
private fun StatsRow(noteCount: Int, taskCount: Int, quickTaskCount: Int, personCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            icon = Icons.Default.NoteAlt,
            label = "Notes",
            value = noteCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.CheckCircle,
            label = "Tasks",
            value = taskCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.AddTask,
            label = "Quick",
            value = quickTaskCount.toString(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Person,
            label = "People",
            value = personCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.tags.joinToString(", ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (note.updatedAt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatRelativeTime(note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (task.status) {
                "pending" -> MaterialTheme.colorScheme.tertiary
                "in-progress" -> MaterialTheme.colorScheme.primary
                "completed" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.outline
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = task.icon.ifEmpty { "T" })
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    StatusBadge(status = task.status)
                }
                if (task.location.isNotEmpty()) {
                    Text(task.location, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.updatedAt.isNotEmpty()) {
                    Text(
                        text = formatRelativeTime(task.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
