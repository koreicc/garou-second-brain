package com.secondbrain.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secondbrain.di.AppModule
import com.secondbrain.domain.model.LinkedEntityInfo

@Composable
fun LinkedEntitiesView(
    linkIds: List<String>,
    onNavigateToNote: (String) -> Unit,
    onNavigateToTask: (String) -> Unit,
    onNavigateToPerson: (String) -> Unit
) {
    if (linkIds.isEmpty()) return

    var linkedEntities by remember { mutableStateOf<List<LinkedEntityInfo>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(linkIds) {
        isLoading = true
        val result = AppModule.linkingRepository.resolveLinks(linkIds)
        linkedEntities = result.getOrNull()
        isLoading = false
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Linked Entities",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
                linkedEntities.isNullOrEmpty() -> {
                    Text(
                        text = "No linked entities found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(linkedEntities!!, key = { it.id }) { entity ->
                            LinkedEntityCard(
                                entity = entity,
                                onClick = {
                                    when (entity.type) {
                                        "note" -> onNavigateToNote(entity.id)
                                        "task" -> onNavigateToTask(entity.id)
                                        "person" -> onNavigateToPerson(entity.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedEntityCard(
    entity: LinkedEntityInfo,
    onClick: () -> Unit
) {
    val (icon: ImageVector, accentColor: Color) = when (entity.type) {
        "note" -> Icons.Default.NoteAlt to MaterialTheme.colorScheme.tertiary
        "task" -> Icons.Default.TaskAlt to MaterialTheme.colorScheme.primary
        else -> Icons.Default.People to MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.08f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = entity.type,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View ${entity.type}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
