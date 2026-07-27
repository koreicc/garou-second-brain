package com.secondbrain.ui.common

import androidx.compose.runtime.Composable

/**
 * Passthrough wrapper (animations disabled).
 * Content renders immediately with no entrance animation.
 */
@Composable
fun AnimatedSection(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    content()
}
