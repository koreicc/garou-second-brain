package com.secondbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 shape system with 8 levels from extraSmall to extraLarge.
 *
 * Component mappings:
 * - extraSmall (4dp): TextFields, dropdown menus
 * - small (8dp): Chips, switches
 * - medium (12dp): Cards, small FABs
 * - large (16dp): Extended FABs, dialogs
 * - extraLarge (28dp): Large FABs, bottom sheets
 */
val SecondBrainShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
