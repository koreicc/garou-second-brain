package com.secondbrain.ui.workspace

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.secondbrain.domain.model.Note
import com.secondbrain.domain.model.Person
import com.secondbrain.domain.model.Task
import com.secondbrain.domain.model.Habit
import com.secondbrain.ui.theme.pillShape
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.StatusBadge
import com.secondbrain.ui.util.formatRelativeTime
import com.secondbrain.ui.util.resolveIcon

private val tabTitles = listOf("Notes", "Tasks", "People", "Habits")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    onNavigateToNoteDetail: (String) -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToPersonDetail: (String) -> Unit,
    onNavigateToNoteEdit: () -> Unit,
    onNavigateToTaskEdit: () -> Unit,
    onNavigateToPersonEdit: () -> Unit,
    onNavigateToHabitDetail: (String) -> Unit,
    onNavigateToHabitEdit: () -> Unit
) {
    val viewModel: WorkspaceViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return WorkspaceViewModel(
                    noteRepository = AppModule.noteRepository,
                    taskRepository = AppModule.taskRepository,
                    personRepository = AppModule.personRepository,
                    habitRepository = AppModule.habitRepository
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

    // Reload silently every time the screen resumes (no loading flicker)
    RefreshOnResume {
        viewModel.silentReload()
    }

    // Delete confirmation dialog
    if (state.showDeleteDialog) {
        val dialogTitle: String
        val dialogText: String
        when {
            state.pendingDeleteNote != null -> {
                dialogTitle = "Delete Note"
                dialogText = "Are you sure you want to delete \"${state.pendingDeleteNote!!.title.ifEmpty { "Untitled" }}\"? This action cannot be undone."
            }
            state.pendingDeleteTask != null -> {
                dialogTitle = "Delete Task"
                dialogText = "Are you sure you want to delete \"${state.pendingDeleteTask!!.title}\"? This action cannot be undone."
            }
            state.pendingDeletePerson != null -> {
                dialogTitle = "Delete Person"
                dialogText = "Are you sure you want to delete \"${state.pendingDeletePerson!!.name}\"? This action cannot be undone."
            }
            state.pendingDeleteHabit != null -> {
                dialogTitle = "Delete Habit"
                dialogText = "Are you sure you want to delete \"${state.pendingDeleteHabit!!.title}\"? This action cannot be undone."
            }
            else -> {
                dialogTitle = "Delete"
                dialogText = "Are you sure you want to delete this item? This action cannot be undone."
            }
        }
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(WorkspaceEvent.DismissDelete) },
            title = { Text(dialogTitle) },
            text = { Text(dialogText) },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(WorkspaceEvent.ConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(WorkspaceEvent.DismissDelete) }) {
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
                title = {
                    Text(
                        text = "Workspace",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = transparentTopAppBarColors(),
                actions = {
                    when (state.selectedTab) {
                        0 -> IconButton(onClick = onNavigateToNoteEdit) {
                            Icon(Icons.Filled.Add, contentDescription = "Add note")
                        }
                        1 -> IconButton(onClick = onNavigateToTaskEdit) {
                            Icon(Icons.Filled.Add, contentDescription = "Add task")
                        }
                        2 -> IconButton(onClick = onNavigateToPersonEdit) {
                            Icon(Icons.Filled.Add, contentDescription = "Add person")
                        }
                        3 -> IconButton(onClick = onNavigateToHabitEdit) {
                            Icon(Icons.Filled.Add, contentDescription = "Add habit")
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
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onEvent(WorkspaceEvent.UpdateSearchQuery(it)) },
                placeholder = { Text("Search ${tabTitles[state.selectedTab].lowercase()}...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onEvent(WorkspaceEvent.UpdateSearchQuery("")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tab row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = state.selectedTab == index,
                        onClick = { viewModel.onEvent(WorkspaceEvent.SelectTab(index)) },
                        text = { Text(title) }
                    )
                }
            }

            // Content area
            when {
                state.isLoading && state.notes.isEmpty() && state.tasks.isEmpty() && state.people.isEmpty() && state.habits.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    when (state.selectedTab) {
                        0 -> NotesTabContent(
                            notes = state.notes,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            onNoteClick = onNavigateToNoteDetail,
                            onDeleteNote = { note -> viewModel.onEvent(WorkspaceEvent.ShowDeleteNote(note)) },
                            onAddNote = onNavigateToNoteEdit
                        )
                        1 -> TasksTabContent(
                            tasks = state.tasks,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            onTaskClick = onNavigateToTaskDetail,
                            onDeleteTask = { task -> viewModel.onEvent(WorkspaceEvent.ShowDeleteTask(task)) },
                            onAddTask = onNavigateToTaskEdit
                        )
                        2 -> PeopleTabContent(
                            people = state.people,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            onPersonClick = onNavigateToPersonDetail,
                            onDeletePerson = { person -> viewModel.onEvent(WorkspaceEvent.ShowDeletePerson(person)) },
                            onAddPerson = onNavigateToPersonEdit
                        )
                        3 -> HabitsTabContent(
                            habits = state.habits,
                            searchQuery = state.searchQuery,
                            isLoading = state.isLoading,
                            onHabitClick = onNavigateToHabitDetail,
                            onDeleteHabit = { habit -> viewModel.onEvent(WorkspaceEvent.ShowDeleteHabit(habit)) },
                            onAddHabit = onNavigateToHabitEdit
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Notes Tab
// ============================================================================

@Composable
private fun NotesTabContent(
    notes: List<Note>,
    searchQuery: String,
    isLoading: Boolean,
    onNoteClick: (String) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onAddNote: () -> Unit
) {
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) {
            notes
        } else {
            val query = searchQuery.trim().lowercase()
            notes.filter { note ->
                note.title.lowercase().contains(query) ||
                    note.tags.any { it.lowercase().contains(query) } ||
                    note.body.lowercase().contains(query)
            }
        }
    }

    if (filteredNotes.isEmpty() && !isLoading) {
        EmptyTabContent(
            icon = { Icon(Icons.Default.NoteAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = if (searchQuery.isNotBlank()) "No matching notes" else "No notes yet",
            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Create your first note to get started",
            buttonLabel = "Create Note",
            onButtonClick = onAddNote
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredNotes, key = { it.id }) { note ->
                WorkspaceNoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onDelete = { onDeleteNote(note) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkspaceNoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
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
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (note.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            note.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("#$tag") },
                                    shape = pillShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                    if (note.updatedAt.isNotEmpty()) {
                        Text(
                            text = formatRelativeTime(note.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete note: ${note.title}"
                    )
                }
            }
        }
    }
}

// ============================================================================
// Tasks Tab
// ============================================================================

@Composable
private fun TasksTabContent(
    tasks: List<Task>,
    searchQuery: String,
    isLoading: Boolean,
    onTaskClick: (String) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onAddTask: () -> Unit
) {
    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) {
            tasks
        } else {
            val query = searchQuery.trim().lowercase()
            tasks.filter { task ->
                task.title.lowercase().contains(query) ||
                    task.location.lowercase().contains(query) ||
                    task.tags.any { it.lowercase().contains(query) }
            }
        }
    }

    if (filteredTasks.isEmpty() && !isLoading) {
        EmptyTabContent(
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = if (searchQuery.isNotBlank()) "No matching tasks" else "No tasks yet",
            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Add a task to start tracking your work",
            buttonLabel = "Create Task",
            onButtonClick = onAddTask
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTasks, key = { it.id }) { task ->
                WorkspaceTaskCard(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onDelete = { onDeleteTask(task) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkspaceTaskCard(
    task: Task,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
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
        val (statusColor, onStatusColor) = when (task.displayStatus) {
            "pending" -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
            "in-progress" -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            "completed" -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
            else -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.onSurface
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
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(status = task.displayStatus)
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

// ============================================================================
// People Tab
// ============================================================================

@Composable
private fun PeopleTabContent(
    people: List<Person>,
    searchQuery: String,
    isLoading: Boolean,
    onPersonClick: (String) -> Unit,
    onDeletePerson: (Person) -> Unit,
    onAddPerson: () -> Unit
) {
    val filteredPeople = remember(people, searchQuery) {
        if (searchQuery.isBlank()) {
            people
        } else {
            val query = searchQuery.trim().lowercase()
            people.filter { person ->
                person.name.lowercase().contains(query) ||
                    person.contacts.any { it.value.lowercase().contains(query) } ||
                    person.tags.any { it.lowercase().contains(query) }
            }
        }
    }

    if (filteredPeople.isEmpty() && !isLoading) {
        EmptyTabContent(
            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = if (searchQuery.isNotBlank()) "No matching people" else "No contacts yet",
            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Add people to build your network",
            buttonLabel = "Add Person",
            onButtonClick = onAddPerson
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPeople, key = { it.id }) { person ->
                WorkspacePersonCard(
                    person = person,
                    onClick = { onPersonClick(person.id) },
                    onDelete = { onDeletePerson(person) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkspacePersonCard(
    person: Person,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
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
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = person.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (person.contacts.isNotEmpty()) {
                        Text(
                            text = person.contacts.first().value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (person.updatedAt.isNotEmpty()) {
                        Text(
                            text = formatRelativeTime(person.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete person: ${person.name}"
                    )
                }
            }
        }
    }
}

// ============================================================================
// Habits Tab
// ============================================================================

@Composable
private fun HabitsTabContent(
    habits: List<Habit>,
    searchQuery: String,
    isLoading: Boolean,
    onHabitClick: (String) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onAddHabit: () -> Unit
) {
    val filteredHabits = remember(habits, searchQuery) {
        if (searchQuery.isBlank()) {
            habits
        } else {
            val query = searchQuery.trim().lowercase()
            habits.filter { habit ->
                habit.title.lowercase().contains(query) ||
                    habit.tags.any { it.lowercase().contains(query) }
            }
        }
    }

    if (filteredHabits.isEmpty() && !isLoading) {
        EmptyTabContent(
            icon = { Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            title = if (searchQuery.isNotBlank()) "No matching habits" else "No habits yet",
            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Create your first habit to start tracking",
            buttonLabel = "Create Habit",
            onButtonClick = onAddHabit
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredHabits, key = { it.id }) { habit ->
                WorkspaceHabitCard(
                    habit = habit,
                    onClick = { onHabitClick(habit.id) },
                    onDelete = { onDeleteHabit(habit) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun WorkspaceHabitCard(
    habit: Habit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (habit.todayCompleted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
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
                color = if (habit.todayCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 0.dp, bottomEnd = 0.dp)
            ) {}

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val iconVector = resolveIcon(habit.icon)
                        if (iconVector != null) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = habit.icon,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else if (habit.icon.isNotEmpty()) {
                            Text(
                                text = habit.icon,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (habit.icon.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (habit.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            habit.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { },
                                    label = { Text("#$tag") },
                                    shape = pillShape,
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                    // Day-of-week dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        dayLabels.forEachIndexed { index, label ->
                            val isActive = habit.daysOfWeek.contains(index + 1)
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete habit: ${habit.title}"
                    )
                }
            }
        }
    }
}

// ============================================================================
// Common Empty State
// ============================================================================

@Composable
private fun EmptyTabContent(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    buttonLabel: String,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        FilledTonalButton(onClick = onButtonClick) {
            Text(buttonLabel)
        }
    }
}
