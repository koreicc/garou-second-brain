package com.secondbrain.ui.util

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.secondbrain.ui.theme.pillShape

/**
 * A colored badge that displays the status of a task.
 *
 * Maps status strings to human-readable labels with matching colors:
 * - "pending" -> "Pending" (tertiary)
 * - "in-progress" -> "In Progress" (primary)
 * - "completed" -> "Done" (secondary)
 * - anything else -> raw status string (outline)
 *
 * Uses pill shape (50% corners) from the Bikram Design DNA system.
 */
@Composable
fun StatusBadge(status: String) {
    val (label, containerColor, contentColor) = when (status) {
        "pending" -> Triple(
            "Pending",
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary
        )
        "in-progress" -> Triple(
            "In Progress",
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary
        )
        "completed" -> Triple(
            "Done",
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.secondary
        )
        else -> Triple(
            status,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.outline
        )
    }
    Surface(
        shape = pillShape,
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

/**
 * A small colored badge that displays priority level.
 * Shown before the status badge on task cards.
 */
@Composable
fun PriorityBadge(priority: String) {
    if (priority.isBlank()) return

    val (label, containerColor, contentColor) = when (priority) {
        "low" -> Quatro(
            "Low",
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.tertiary
        )
        "medium" -> Quatro(
            "Med",
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.secondary
        )
        "high" -> Quatro(
            "High",
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error
        )
        "urgent" -> Quatro(
            "Urg",
            MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.error
        )
        else -> Quatro(
            priority,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.outline
        )
    }
    Surface(
        shape = pillShape,
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private data class Quatro(
    val first: String,
    val second: androidx.compose.ui.graphics.Color,
    val third: androidx.compose.ui.graphics.Color
)
