package com.secondbrain.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.secondbrain.data.ThemeState

val LocalSnackbarHostState = staticCompositionLocalOf<androidx.compose.material3.SnackbarHostState> {
    error("No SnackbarHostState provided")
}

val LocalIsDark = staticCompositionLocalOf { false }

/** When true, the root scaffold uses a gradient background (primaryContainer -> surface). */
val LocalUseGradient = compositionLocalOf { false }

/** When true, note/task cards show hero/card images. */
val LocalHeroOnCards = compositionLocalOf { false }

/** When true, app bars use frosted-glass blur effect. */
val LocalBlurBars = compositionLocalOf { true }

/** When true, surface containers pick up extra color tinting toward the accent. */
val LocalUseEnhancedShading = compositionLocalOf { false }

/** When true, animations use snap() instead of tween/spring. */
val LocalReducedMotion = compositionLocalOf { false }

/** Full ThemeState for downstream screens to read synchronously. */
val LocalThemeState = compositionLocalOf { ThemeState() }
