package com.secondbrain.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onTaskClick: (String) -> Unit,
    onAddTask: () -> Unit
) {
    val viewModel: TaskListViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TaskListViewModel(taskRepository = AppModule.taskRepository) as T
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

    // Reload tasks every time this screen becomes visible
    RefreshOnResume {
        viewModel.onEvent(TaskListEvent.LoadTasks)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tasks",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = onAddTask) {
                        Icon(Icons.Filled.Add, contentDescription = "Add task")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.tasks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tasks yet", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskClick(task.id) },
                            onDelete = { viewModel.onEvent(TaskListEvent.DeleteTask(task.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(status = task.status)
                }
                if (task.location.isNotEmpty()) {
                    Text(
                        text = task.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (task.updatedAt.isNotEmpty()) {
                    Text(
                        text = formatRelativeTime(task.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete task: ${task.title}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
