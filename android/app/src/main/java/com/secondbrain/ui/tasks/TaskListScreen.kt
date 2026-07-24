package com.secondbrain.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.secondbrain.ui.util.PriorityBadge
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.resolveIcon

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var showFilters by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    RefreshOnResume {
        viewModel.silentReload()
    }

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
                        text = if (state.isSelectionMode) {
                            "${state.selectedIds.size} selected"
                        } else {
                            "Tasks"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (state.isSelectionMode) {
                        IconButton(onClick = { viewModel.onEvent(TaskListEvent.SelectAll) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(TaskListEvent.BatchComplete) },
                            enabled = state.selectedIds.isNotEmpty() && !state.isBatchLoading
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Complete selected")
                        }
                        IconButton(
                            onClick = { viewModel.onEvent(TaskListEvent.BatchDelete) },
                            enabled = state.selectedIds.isNotEmpty() && !state.isBatchLoading
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                        IconButton(onClick = { viewModel.onEvent(TaskListEvent.ToggleSelectionMode) }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    } else {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        IconButton(onClick = { viewModel.onEvent(TaskListEvent.ToggleSelectionMode) }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Select tasks")
                        }
                        IconButton(onClick = onAddTask) {
                            Icon(Icons.Filled.Add, contentDescription = "Add task")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            if (showSearch) {
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onEvent(TaskListEvent.SetSearchQuery(it)) },
                    onClose = {
                        showSearch = false
                        viewModel.onEvent(TaskListEvent.SetSearchQuery(""))
                    }
                )
            }

            // Filter chips
            if (showFilters) {
                FilterBar(
                    statusFilter = state.statusFilter,
                    priorityFilter = state.priorityFilter,
                    sortBy = state.sortBy,
                    sortOrder = state.sortOrder,
                    onStatusFilter = { viewModel.onEvent(TaskListEvent.SetStatusFilter(it)) },
                    onPriorityFilter = { viewModel.onEvent(TaskListEvent.SetPriorityFilter(it)) },
                    onSortBy = { viewModel.onEvent(TaskListEvent.SetSortBy(it)) },
                    onToggleSortOrder = { viewModel.onEvent(TaskListEvent.ToggleSortOrder) }
                )
            }

            // Batch loading indicator
            if (state.isBatchLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Task list
            when {
                state.isLoading && state.tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.tasks.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
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
                            text = if (state.searchQuery.isNotEmpty() || state.statusFilter.isNotEmpty()) {
                                "No tasks match your filters"
                            } else {
                                "Add a task to start tracking your work"
                            },
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
                    val timeBucketLabels = mapOf(
                        0 to "Overdue",
                        1 to "Today",
                        2 to "Tomorrow",
                        3 to "This Week",
                        4 to "Later",
                        5 to "Anytime",
                        6 to "Completed"
                    )
                    val orderedBuckets = listOf(0, 1, 2, 3, 4, 5, 6)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        orderedBuckets.forEach { bucket ->
                            val tasksInBucket = state.groupedTasks[bucket]
                            if (!tasksInBucket.isNullOrEmpty()) {
                                stickyHeader(key = "header_$bucket") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 0.dp
                                    ) {
                                        Text(
                                            text = timeBucketLabels[bucket] ?: "Other",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                items(tasksInBucket, key = { it.id }) { task ->
                                    TaskCard(
                                        task = task,
                                        isSelectionMode = state.isSelectionMode,
                                        isSelected = task.id in state.selectedIds,
                                        onClick = {
                                            if (state.isSelectionMode) {
                                                viewModel.onEvent(TaskListEvent.ToggleSelection(task.id))
                                            } else {
                                                onTaskClick(task.id)
                                            }
                                        },
                                        onLongClick = {
                                            if (!state.isSelectionMode) {
                                                viewModel.onEvent(TaskListEvent.ToggleSelectionMode)
                                                viewModel.onEvent(TaskListEvent.ToggleSelection(task.id))
                                            }
                                        },
                                        onDelete = { viewModel.onEvent(TaskListEvent.ShowDeleteConfirmation(task)) }
                                    )
                                }
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search tasks...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close search")
            }
        }
    }
}

@Composable
private fun FilterBar(
    statusFilter: String,
    priorityFilter: String,
    sortBy: String,
    sortOrder: String,
    onStatusFilter: (String) -> Unit,
    onPriorityFilter: (String) -> Unit,
    onSortBy: (String) -> Unit,
    onToggleSortOrder: () -> Unit
) {
    val statuses = listOf("" to "All", "pending" to "Pending", "in-progress" to "In Progress", "completed" to "Done")
    val priorities = listOf("" to "All", "" to "---", "low" to "Low", "medium" to "Med", "high" to "High", "urgent" to "Urg")
    val sortOptions = listOf("created_at" to "Created", "updated_at" to "Updated", "due_date" to "Due", "title" to "Title", "priority" to "Priority")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Status filter
            Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                statuses.forEach { (value, label) ->
                    FilterChip(
                        selected = statusFilter == value,
                        onClick = { onStatusFilter(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Priority filter
            Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                priorities.forEach { (value, label) ->
                    FilterChip(
                        selected = priorityFilter == value,
                        onClick = { onPriorityFilter(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        enabled = value.isNotEmpty() || value == "",
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sort
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sort", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sortOptions.forEach { (value, label) ->
                    FilterChip(
                        selected = sortBy == value,
                        onClick = { onSortBy(value) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
                IconButton(onClick = onToggleSortOrder, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = if (sortOrder == "asc") "Ascending" else "Descending",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: Task,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusColor, onStatusColor) = when (task.displayStatus) {
        "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        "in-progress" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        "completed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        "expired" -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
        else -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.onSurface
    }

    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        label = "card_color"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
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
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val iconVector = resolveIcon(task.icon)
                        if (iconVector != null) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = task.icon,
                                modifier = Modifier.size(24.dp),
                                tint = onStatusColor
                            )
                        } else {
                            Text(
                                text = if (task.icon.isNotEmpty()) task.icon.take(1).uppercase() else task.title.take(1).uppercase(),
                                color = onStatusColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PriorityBadge(priority = task.priority)
                            StatusBadge(status = task.displayStatus)
                        }
                    }
                    if (task.location.isNotEmpty()) {
                        Text(
                            text = task.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Subtask progress
                    if (task.subtasks.isNotEmpty()) {
                        val completedCount = task.subtasks.count { it.completed }
                        val totalCount = task.subtasks.size
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { completedCount.toFloat() / totalCount.toFloat() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp),
                                color = statusColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$completedCount/$totalCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Date info
                    val dateInfo = when (task.dateMode) {
                        "due_date" -> if (task.dueDate.isNotEmpty()) "Due: ${task.dueDate.take(10)}" else ""
                        "range" -> {
                            val s = task.startDate.take(10)
                            val e = task.endDate.take(10)
                            if (s.isNotEmpty() && e.isNotEmpty()) "$s - $e" else if (s.isNotEmpty()) s else e
                        }
                        else -> ""
                    }
                    if (dateInfo.isNotEmpty()) {
                        Text(
                            text = dateInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!isSelectionMode) {
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
}