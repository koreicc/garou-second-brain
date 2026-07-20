package com.secondbrain.ui.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A colored badge that displays the status of a task.
 *
 * Maps status strings to human-readable labels with matching colors:
 * - "pending" -> "Pending" (tertiary)
 * - "in-progress" -> "In Progress" (primary)
 * - "completed" -> "Done" (secondary)
 * - anything else -> raw status string (outline)
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
        shape = MaterialTheme.shapes.extraSmall,
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
