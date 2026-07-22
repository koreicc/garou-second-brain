package com.secondbrain.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

// ============================================================================
// Data
// ============================================================================

private data class NavPillItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Screen
)

private val navPillItems = listOf(
    NavPillItem(
        label = "Dashboard",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        route = Screen.Dashboard
    ),
    NavPillItem(
        label = "Workspace",
        selectedIcon = Icons.Filled.Description,
        unselectedIcon = Icons.Outlined.Description,
        route = Screen.Workspace
    ),
    NavPillItem(
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        route = Screen.Settings
    )
)

// ============================================================================
// Spring animation spec matching Remember's bouncy style
// ============================================================================

private fun pillSpringSpec() = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

private fun pillColorSpringSpec() = spring<androidx.compose.ui.graphics.Color>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// ============================================================================
// SecondBrainBottomBar -- floating pill + FAB (Remember-style)
// ============================================================================

/**
 * Floating pill-shaped bottom navigation inspired by Bikram Agarwal's Remember app.
 *
 * A compact pill with 3 navigation tabs. The selected tab expands to show its label
 * with a bouncy spring animation; unselected tabs show only their icon.
 * A separate FAB is positioned to the right of the pill.
 */
@Composable
fun SecondBrainBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (Screen) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating pill - wraps content width (no fillMaxWidth)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navPillItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavPillTab(
                            item = item,
                            selected = selected,
                            onClick = { onTabSelected(item.route) }
                        )
                    }
                }
            }

            // Separate FAB next to the pill
            FloatingActionButton(
                onClick = onFabClick,
                modifier = Modifier.size(56.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

// ============================================================================
// Nav pill tab item - Remember-style animated width
// ============================================================================

/**
 * A single navigation tab inside the floating pill.
 *
 * - Selected: icon + label, with a filled background
 * - Unselected: icon only, transparent background
 * - The label smoothly expands/shrinks with a spring animation
 * - No layout shift because each tab has its own natural width (48dp + labelWidth)
 *   and the parent Row does NOT use SpaceEvenly or weight()
 */
@Composable
private fun NavPillTab(
    item: NavPillItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animate label width: 72dp when selected, 0dp when not (Remember-style)
    // Uses animateFloatAsState and converts to Dp to avoid a Kotlin Compose
    // compiler plugin issue with Dp.compareTo on this Kotlin version
    val labelWidthFraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = pillSpringSpec(),
        label = "nav_label_width_fraction"
    )
    val labelWidth = labelWidthFraction * 72.dp

    // Animate container color
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = pillColorSpringSpec(),
        label = "pill_tab_bg"
    )

    // Animate content color
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = pillColorSpringSpec(),
        label = "pill_tab_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .height(48.dp)
            .width(48.dp + labelWidth)
            .semantics { contentDescription = item.label }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = if (selected) 6.dp else 0.dp)
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )

            // Label only renders when labelWidth exceeds a small threshold
            // This prevents layout shifts while allowing smooth width animation
            if (labelWidthFraction > 0.05f) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
