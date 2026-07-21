package com.secondbrain.ui.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for responsive breakpoints, adapted from Bikram Design DNA.
 */

/** Below this height (dp) in landscape, screens use compact spacing. */
private const val SMALL_LANDSCAPE_HEIGHT_DP = 480

/**
 * Action layout direction: horizontal row or stacked column.
 */
enum class ResponsiveActionLayout {
    HORIZONTAL,
    STACKED,
}

/**
 * Adjust text scale based on available width for responsive typography.
 */
fun responsiveTextScaleForWidth(availableWidth: Dp): Float = when {
    availableWidth < 320.dp -> 0.84f
    availableWidth < 360.dp -> 0.88f
    availableWidth < 430.dp -> 0.93f
    else -> 1f
}

/**
 * Decide whether action buttons should stack vertically based on available width
 * and font scale.
 */
fun responsiveActionLayout(
    availableWidth: Dp,
    effectiveFontScale: Float,
    itemCount: Int,
): ResponsiveActionLayout {
    if (itemCount <= 1) return ResponsiveActionLayout.HORIZONTAL
    val tooNarrowForRow = availableWidth < 360.dp ||
        (availableWidth < 430.dp && effectiveFontScale > 1.10f) ||
        (availableWidth < 520.dp && effectiveFontScale > 1.15f)
    return if (tooNarrowForRow) ResponsiveActionLayout.STACKED else ResponsiveActionLayout.HORIZONTAL
}

/**
 * Number of columns for a grid/mosaic layout based on available width.
 */
fun mosaicColumnCount(availableWidth: Dp): Int = when {
    availableWidth < 340.dp -> 1
    availableWidth < 960.dp -> 2
    availableWidth < 1280.dp -> 3
    else -> 4
}

@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

/**
 * True in landscape on a short window (e.g. phones rotated, small split-screen).
 */
@Composable
@ReadOnlyComposable
fun isSmallLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.screenHeightDp < SMALL_LANDSCAPE_HEIGHT_DP
}

/**
 * Spacing values for empty-state layouts.
 */
@Immutable
data class EmptyStateSpacing(
    val titleSpacer: Dp,
    val subtitleSpacer: Dp,
)

/**
 * Spacing ladder for empty states, tighter on small landscape windows.
 */
@Composable
@ReadOnlyComposable
fun emptyStateSpacing(prominent: Boolean): EmptyStateSpacing {
    val small = isSmallLandscape()
    return if (prominent) {
        EmptyStateSpacing(
            titleSpacer = if (small) 12.dp else 24.dp,
            subtitleSpacer = if (small) 4.dp else 8.dp,
        )
    } else {
        EmptyStateSpacing(
            titleSpacer = if (small) 10.dp else 18.dp,
            subtitleSpacer = if (small) 4.dp else 6.dp,
        )
    }
}
