package com.secondbrain.ui.common

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.secondbrain.ui.theme.LocalReducedMotion

/**
 * Entrance animation wrapper for screen sections.
 *
 * On first composition the content is laid out normally (no layout collapse),
 * then fades and slides in with a smooth spring motion.
 * Respects the system reduced-motion setting.
 *
 * @param index Optional stagger index — each section delays by index * 30ms
 *              for a brief cascading effect.
 * @param content The composable content to animate.
 */
@Composable
fun AnimatedSection(
    index: Int = 0,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 120,
            delayMillis = index * 30
        ),
        label = "section_alpha_$index"
    )

    val slidePx = with(density) { 48.dp.toPx() }
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "section_offset_$index"
    )

    val animationFinished = visible && animatedAlpha >= 0.99f

    if (reducedMotion || animationFinished) {
        content()
    } else {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    alpha = animatedAlpha
                    translationY = animatedOffsetY * slidePx
                }
        ) {
            content()
        }
    }
}
