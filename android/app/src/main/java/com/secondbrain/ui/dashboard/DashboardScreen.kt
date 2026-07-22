package com.secondbrain.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.QuickTask
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
private fun QuickTaskRow(
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
                    val calendar = Calendar.getInstance()
                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
                    val greeting = when {
                        hour < 12 -> "Good morning"
                        hour < 17 -> "Good afternoon"
                        else -> "Good evening"
                    }
                    val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                    val dateString = dateFormat.format(calendar.time)

                    Column {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = dateString,
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
            item {
                Spacer(modifier = Modifier.height(8.dp))
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
                    SectionHeader(title = "Quick Tasks (${state.quickTasks.size})")
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
                    SectionHeader(title = "Recent Notes", onSeeAll = onNavigateToNotes)
                }
                items(state.recentNotes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { onNavigateToNoteDetail(note.id) })
                }
            }

            if (state.recentTasks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Active Tasks", onSeeAll = onNavigateToTasks)
                }
                items(state.recentTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onClick = { onNavigateToTaskDetail(task.id) })
                }
            }

            if (state.recentNotes.isEmpty() && state.recentTasks.isEmpty() && !state.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Dashboard,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Welcome to Second Brain",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Start by creating notes, tasks, or quick tasks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all")
            }
        }
    }
}

@Composable
private fun QuickTaskCard(onAddQuickTask: (String) -> Unit) {
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
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Task", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            icon = Icons.Default.People,
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
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary)
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp).weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (note.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.body.lines().firstOrNull { it.isNotBlank() } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (note.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        note.tags.forEach { tag ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text("#$tag") }
                            )
                        }
                    }
                }
                if (note.updatedAt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatRelativeTime(note.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp), 
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
                color = statusColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = task.icon.ifEmpty { "T" },
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(status = task.status)
                }
                if (task.location.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(task.location, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (task.updatedAt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
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
