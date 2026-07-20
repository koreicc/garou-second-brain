package com.secondbrain.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Subtask
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.WikilinkText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: TaskDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TaskDetailViewModel(
                    taskRepository = AppModule.taskRepository,
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

    // Reload task data when navigating back from edit screen
    RefreshOnResume {
        viewModel.reload()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
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
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (task.location.isNotEmpty()) {
                        Text("Location: ${task.location}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (task.startDate.isNotEmpty() || task.endDate.isNotEmpty()) {
                        Text(
                            "Dates: ${task.startDate.take(10)} - ${task.endDate.take(10)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (task.tags.isNotEmpty()) {
                        Text(
                            task.tags.joinToString(", ") { "#$it" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (task.subtasks.isNotEmpty()) {
                        Text("Subtasks", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        task.subtasks.forEach { subtask ->
                            SubtaskRow(
                                subtask = subtask,
                                onCheckedChange = { viewModel.onEvent(TaskDetailEvent.ToggleSubtask(subtask.id)) }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    if (task.body.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        WikilinkText(
                            text = task.body,
                            onWikilinkClick = { target ->
                                snackbarHostState.showSnackbar("WikiLink: $target")
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubtaskRow(subtask: Subtask, onCheckedChange: ((Boolean) -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = subtask.completed,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (subtask.completed) TextDecoration.LineThrough else TextDecoration.None
        )
    }
}
