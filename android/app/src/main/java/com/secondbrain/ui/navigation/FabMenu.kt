package com.secondbrain.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * An overlay menu that appears when the FAB is tapped.
 * Shows creation options: New Note, New Task, New Person.
 *
 * @param isVisible Whether the menu is shown
 * @param onDismiss Called when the backdrop is tapped to close
 * @param onNewNote Called when "New Note" is selected
 * @param onNewTask Called when "New Task" is selected
 * @param onNewPerson Called when "New Person" is selected
 * @param onNewHabit Called when "New Habit" is selected
 */
@Composable
fun FabMenuOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNewNote: () -> Unit,
    onNewTask: () -> Unit,
    onNewPerson: () -> Unit,
    onNewHabit: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200))
    ) {
        // Semi-transparent backdrop filling the entire screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
            ) {
            // Menu cards aligned above the bottom bar with staggered spring reveal
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val menuItems = listOf(
                    Triple("New Note", Icons.Default.Description to MaterialTheme.colorScheme.tertiary) { onDismiss(); onNewNote() },
                    Triple("New Task", Icons.Default.Task to MaterialTheme.colorScheme.primary) { onDismiss(); onNewTask() },
                    Triple("New Person", Icons.Default.Person to MaterialTheme.colorScheme.secondary) { onDismiss(); onNewPerson() },
                    Triple("New Habit", Icons.Default.Repeat to MaterialTheme.colorScheme.tertiary) { onDismiss(); onNewHabit() }
                )
                menuItems.forEachIndexed { index, (label, iconTint, onClick) ->
                    val (icon, tint) = iconTint
                    StaggeredFabMenuItem(
                        index = index,
                        label = label,
                        icon = icon,
                        tint = tint,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

/**
 * A single FAB menu option with staggered spring entrance animation.
 * Each item slides up and fades in with a delay based on its index.
 */
@Composable
private fun StaggeredFabMenuItem(
    index: Int,
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    val delayMillis = index * 50

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 200,
            delayMillis = delayMillis
        ),
        label = "fab_alpha_$index"
    )
    val offsetY by animateFloatAsState(
        targetValue = 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_offset_$index"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY * density
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Create a new ${label.removePrefix("New ").lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A single menu option card inside the FAB expansion menu.
 *
 * @param label Display text (e.g. "New Note")
 * @param icon Icon for the option
 * @param tint Accent color for the icon and its circular background
 * @param onClick Called when the card is tapped
 */
