package com.secondbrain.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.ui.theme.pillShape
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.RefreshOnResume
import com.secondbrain.ui.util.WikilinkText
import com.secondbrain.ui.util.formatRelativeTime
import com.secondbrain.ui.common.AnimatedSection
import com.secondbrain.ui.common.LinkedEntitiesView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteDetailScreen(
    noteId: String,
    onEditClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (String) -> Unit = {},
    onNavigateToTask: (String) -> Unit = {},
    onNavigateToPerson: (String) -> Unit = {}
) {
    val viewModel: NoteDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return NoteDetailViewModel(
                    noteRepository = AppModule.noteRepository,
                    searchRepository = AppModule.searchRepository,
                    noteId = noteId
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

    // Reload note data when navigating back from edit screen
    RefreshOnResume {
        viewModel.reload()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.note?.title ?: "Note",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back to notes")
                    }
                },
                actions = {
                    val isArchived = state.note?.status == "archived"
                    IconButton(onClick = { viewModel.onEvent(NoteDetailEvent.ToggleArchive) }) {
                        Icon(
                            imageVector = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (isArchived) "Restore note" else "Archive note"
                        )
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit note")
                    }
                },
                colors = transparentTopAppBarColors()
            )
        },
        containerColor = Color.Transparent
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
            state.note != null -> {
                val note = state.note!!
                AnimatedSection {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (note.tags.isNotEmpty() || note.createdAt.isNotEmpty() || note.updatedAt.isNotEmpty()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (note.tags.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        note.tags.forEach { tag ->
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
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                if (note.createdAt.isNotEmpty()) {
                                    Text(
                                        text = "Created: ${formatRelativeTime(note.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (note.updatedAt.isNotEmpty() && note.updatedAt != note.createdAt) {
                                    Text(
                                        text = "Updated: ${formatRelativeTime(note.updatedAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (note.links.isNotEmpty()) {
                        LinkedEntitiesView(
                            linkIds = note.links,
                            onNavigateToNote = onNavigateToNote,
                            onNavigateToTask = onNavigateToTask,
                            onNavigateToPerson = onNavigateToPerson
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    
                    if (note.body.isBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.TextSnippet,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No content",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        WikilinkText(
                            text = note.body,
                            onWikilinkClick = { target ->
                                viewModel.onEvent(NoteDetailEvent.ResolveWikilink(target))
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

