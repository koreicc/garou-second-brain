package com.secondbrain.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.secondbrain.ui.theme.LocalReducedMotion

/**
 * Minimal entrance animation wrapper for scroll-based screen sections.
 *
 * Fades and slides content in quickly on first composition.
 * Respects the system reduced-motion setting.
 *
 * @param index Optional stagger index for sequential entrance.
 *              When non-zero the animation delays by index * 30ms.
 * @param content The composable content to animate.
 */
@Composable
fun AnimatedSection(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    if (reducedMotion) {
        content()
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(80, delayMillis = index * 30)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetY = { it / 8 }
                )
        ) {
            content()
        }
    }
}
