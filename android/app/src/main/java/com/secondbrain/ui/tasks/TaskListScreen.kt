package com.secondbrain.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.theme.transparentTopAppBarColors
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

    // Reload tasks silently (no loading flicker)
    RefreshOnResume {
        viewModel.silentReload()
    }

    // Delete confirmation dialog
    if (state.showDeleteDialog && state.pendingDeleteTask != null) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TaskListEvent.DismissDelete) },
            title = { Text("Delete Task") },
            text = {
                Text("Are you sure you want to delete \"${state.pendingDeleteTask!!.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.onEvent(TaskListEvent.ConfirmDelete) }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(TaskListEvent.DismissDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = transparentTopAppBarColors(),
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
            state.isLoading && state.tasks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.tasks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tasks yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add a task to start tracking your work",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FilledTonalButton(onClick = onAddTask) {
                        Text("Create Task")
                    }
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
                            onDelete = { viewModel.onEvent(TaskListEvent.ShowDeleteConfirmation(task)) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        val statusColor = when (task.status) {
            "pending" -> MaterialTheme.colorScheme.tertiary
            "in-progress" -> MaterialTheme.colorScheme.primary
            "completed" -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.outline
        }

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(),
                color = statusColor,
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {}
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val displayIcon = if (task.icon.isNotEmpty()) task.icon else task.title.take(1).uppercase()
                        Text(
                            text = displayIcon,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
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
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete task: ${task.title}"
                    )
                }
            }
        }
    }
}
