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
