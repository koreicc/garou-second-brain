package com.secondbrain.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Consists of a rounded pill containing 3 tab icons, with a separate FAB
 * positioned to the right. Both float above the screen content rather than
 * being attached to the bottom edge.
 *
 * Features animated tab selection: the selected tab expands with a bouncy spring
 * animation to reveal its label, while unselected tabs remain compact with icons only.
 *
 * @param currentDestination Current nav destination to determine selection
 * @param onTabSelected Called when a navigation tab is tapped
 * @param onFabClick Called when the FAB is tapped
 * @param modifier Modifier for the container
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
            .padding(bottom = 28.dp), // Higher position above bottom edge
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Floating pill with 3 animated nav tabs
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
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
// Nav pill tab item - fixed equal width, no layout shift on selection
// ============================================================================

@Composable
private fun NavPillTab(
    item: NavPillItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Animate container color only - NO width animation to prevent layout shifts
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

    // Animate label opacity - text stays in layout to prevent shifting
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = pillSpringSpec(),
        label = "pill_label_alpha"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .height(48.dp)
            .weight(1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )

            // Label always in layout, animates opacity only - prevents shifting
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .alpha(labelAlpha),
                maxLines = 1
            )
        }
    }
}
