package com.secondbrain.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.Task
import com.secondbrain.ui.theme.transparentTopAppBarColors
import com.secondbrain.ui.util.resolveIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepeatingUiState(
    val templates: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface RepeatingEvent {
    data object LoadTemplates : RepeatingEvent
}

class RepeatingViewModel(
    private val taskRepository: com.secondbrain.data.repository.TaskRepository
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(RepeatingUiState())
    val state: StateFlow<RepeatingUiState> = _state.asStateFlow()

    init {
        loadTemplates()
    }

    fun onEvent(event: RepeatingEvent) {
        when (event) {
            is RepeatingEvent.LoadTemplates -> loadTemplates()
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            taskRepository.getAll()
                .onSuccess { tasks ->
                    val templates = tasks.filter { it.isTemplate }
                    _state.update { it.copy(templates = templates, isLoading = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}

private fun formatRecurrencePattern(task: Task): String {
    val rec = task.recurrence ?: return ""
    val interval = rec.interval
    val type = when (rec.type) {
        "daily" -> if (interval == 1) "daily" else "every $interval days"
        "weekly" -> {
            val days = rec.daysOfWeek.map {
                when (it) {
                    1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"
                    5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> ""
                }
            }
            if (days.isEmpty()) {
                if (interval == 1) "weekly" else "every $interval weeks"
            } else {
                val dayStr = days.joinToString(", ")
                if (interval == 1) "Every $dayStr" else "Every $interval weeks on $dayStr"
            }
        }
        "monthly" -> if (interval == 1) "monthly" else "every $interval months"
        "yearly" -> if (interval == 1) "yearly" else "every $interval years"
        else -> rec.type
    }
    val window = if (task.startDate.isNotEmpty() && task.endDate.isNotEmpty()) {
        " until ${task.endDate.take(10)}"
    } else ""
    return "$type$window"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatingScreen(
    onNavigateBack: () -> Unit,
    onTemplateClick: (String) -> Unit
) {
    val viewModel: RepeatingViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RepeatingViewModel(taskRepository = AppModule.taskRepository) as T
            }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Repeating Tasks") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = transparentTopAppBarColors()
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
            state.templates.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No repeating tasks",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create a task with a date range and recurrence to make it repeat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onClick = { onTemplateClick(template.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Task,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val iconVector = resolveIcon(template.icon)
                    if (iconVector != null) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = template.icon,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = template.title.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val pattern = formatRecurrencePattern(template)
                if (pattern.isNotEmpty()) {
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
