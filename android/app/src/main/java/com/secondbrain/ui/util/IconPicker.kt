package com.secondbrain.ui.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A list of commonly used Material Icons for task icons.
 */
data class IconEntry(val name: String, val icon: ImageVector)

private val taskIcons: List<IconEntry> = listOf(
    IconEntry("task", Icons.Default.CheckCircle),
    IconEntry("edit", Icons.Default.Edit),
    IconEntry("create", Icons.Default.Create),
    IconEntry("write", Icons.Default.EditNote),
    IconEntry("book", Icons.Default.Book),
    IconEntry("note", Icons.Default.NoteAlt),
    IconEntry("list", Icons.Default.ListAlt),
    IconEntry("code", Icons.Default.Code),
    IconEntry("alarm", Icons.Default.AccessAlarm),
    IconEntry("schedule", Icons.Default.Schedule),
    IconEntry("timer", Icons.Default.Timer),
    IconEntry("calendar", Icons.Default.CalendarToday),
    IconEntry("date", Icons.Default.DateRange),
    IconEntry("notif", Icons.Default.Notifications),
    IconEntry("refresh", Icons.Default.Refresh),
    IconEntry("search", Icons.Default.Search),
    IconEntry("home", Icons.Default.Home),
    IconEntry("location", Icons.Default.LocationOn),
    IconEntry("flight", Icons.Default.Flight),
    IconEntry("dining", Icons.Default.LocalDining),
    IconEntry("cart", Icons.Default.ShoppingCart),
    IconEntry("camera", Icons.Default.CameraAlt),
    IconEntry("photo", Icons.Default.Photo),
    IconEntry("video", Icons.Default.Videocam),
    IconEntry("music", Icons.Default.MusicNote),
    IconEntry("person", Icons.Default.Person),
    IconEntry("group", Icons.Default.Group),
    IconEntry("phone", Icons.Default.Phone),
    IconEntry("email", Icons.Default.Email),
    IconEntry("chat", Icons.Default.QuestionAnswer),
    IconEntry("thumb", Icons.Default.ThumbUp),
    IconEntry("star", Icons.Default.Star),
    IconEntry("favorite", Icons.Default.Favorite),
    IconEntry("build", Icons.Default.Build),
    IconEntry("settings", Icons.Default.Settings),
    IconEntry("visibility", Icons.Default.Visibility),
    IconEntry("explore", Icons.Default.TravelExplore),
    IconEntry("light", Icons.Default.Lightbulb),
    IconEntry("run", Icons.Default.DirectionsRun),
    IconEntry("warning", Icons.Default.Warning),
    IconEntry("menu", Icons.Default.MenuBook)
)

private val iconNames: Map<String, ImageVector> = taskIcons.associate { it.name to it.icon }

/**
 * Returns the ImageVector for a given icon name, or null if not found.
 */
fun resolveIcon(name: String): ImageVector? = iconNames[name]

/**
 * An icon picker dialog that displays a grid of common task icons.
 *
 * @param currentIcon The currently selected icon name
 * @param onIconSelected Called with the selected icon name
 * @param onDismiss Called when the dialog is dismissed
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IconPickerDialog(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select an Icon")
        },
        text = {
            Column {
                Text(
                    "Choose an icon for your task:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "None" option
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onIconSelected("") },
                        shape = MaterialTheme.shapes.small,
                        color = if (currentIcon.isEmpty())
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("T", fontSize = 20.sp)
                        }
                    }

                    taskIcons.forEach { entry ->
                        val isSelected = currentIcon == entry.name
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onIconSelected(entry.name) },
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = entry.name,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
