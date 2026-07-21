package com.secondbrain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Size
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

val pillShape = RoundedCornerShape(percent = 50)
val compactControlShape = RoundedCornerShape(12.dp)

/**
 * Material 3 shape system from the Bikram Design DNA.
 *
 * Component mappings:
 * - extraSmall (6dp): TextFields, dropdown menus
 * - small (10dp): Chips, switches
 * - medium (14dp): Cards, small FABs
 * - large (18dp): Extended FABs, dialogs
 * - extraLarge (24dp): Bottom sheets
 * - largeIncreased (22dp): Custom card variants
 * - extraLargeIncreased (28dp): Wide panels
 * - extraExtraLarge (36dp): Hero elements
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val SecondBrainShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
    largeIncreased = RoundedCornerShape(22.dp),
    extraLargeIncreased = RoundedCornerShape(28.dp),
    extraExtraLarge = RoundedCornerShape(36.dp),
)

/**
 * Adapts a RoundedPolygon to Compose's Shape contract for clipping and borders.
 */
internal class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = polygon.toPath().asComposePath()
        path.transform(pathBoundsMatrix(path, size))
        return Outline.Generic(path)
    }
}

/**
 * Adapts a Morph (animated transition between two RoundedPolygons) to Compose's Shape.
 */
internal class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress).asComposePath()
        path.transform(pathBoundsMatrix(path, size))
        return Outline.Generic(path)
    }
}

private fun pathBoundsMatrix(
    path: Path,
    size: Size,
): Matrix {
    val bounds = path.getBounds()
    val matrix = Matrix()
    matrix.scale(size.width / bounds.width, size.height / bounds.height)
    matrix.translate(-bounds.left, -bounds.top)
    return matrix
}
