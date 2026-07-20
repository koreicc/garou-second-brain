package com.secondbrain.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.util.RefreshOnResume

@Composable
private fun QuickTaskRow(
    quickTask: QuickTask,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
            }
            Checkbox(
                checked = false,
                onCheckedChange = { onComplete() }
            )
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
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

    // Reload data every time the screen resumes (e.g. navigating back from detail/edit)
    RefreshOnResume {
        viewModel.onEvent(DashboardEvent.LoadData)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(DashboardEvent.LoadData) },
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reload from server"
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
                    quickTaskCount = state.quickTasks.size
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
                    QuickTaskRow(
                        quickTask = qt,
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

            state.error?.let { error ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(text = error, modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTaskCard(onAddQuickTask: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
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
private fun StatsRow(noteCount: Int, taskCount: Int, quickTaskCount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Notes", noteCount.toString(), Modifier.weight(1f))
        StatCard("Tasks", taskCount.toString(), Modifier.weight(1f))
        StatCard("Quick", quickTaskCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(note.title, style = MaterialTheme.typography.titleMedium)
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(note.tags.joinToString(", ") { "#$it" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                if (task.location.isNotEmpty()) {
                    Text(task.location, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
