package com.secondbrain.ui.theme

import android.app.Activity
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import com.materialkolor.rememberDynamicColorScheme
import com.secondbrain.data.ThemeState

/**
 * Main theme composable using Material 3 with dynamic color support,
 * gradient backgrounds, and Bikram Design DNA aesthetics.
 *
 * @param themeState Runtime theme configuration (color source, palette, gradient, etc.)
 * @param paintBackground When false, skips the full-screen gradient (for translucent activities)
 * @param content Child content
 */
@Composable
fun SecondBrainTheme(
    themeState: ThemeState = ThemeState(),
    paintBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = themeState.effectiveDarkTheme(systemDark)
    val blackThemeActive = themeState.useBlackTheme && darkTheme
    val effectiveUseGradient = themeState.useGradient && !blackThemeActive

    val context = LocalContext.current
    val reducedMotion = rememberReducedMotionEnabled(context)

    // Resolve dynamic color scheme based on theme state
    val colorResolution = rememberResolvedColorScheme(
        context = context,
        themeState = themeState,
        darkTheme = darkTheme,
        black = blackThemeActive,
    )

    val targetColorScheme = colorResolution.colorScheme
    val targetBackgroundScheme = colorResolution.backgroundScheme

    val wallpaperTint = rememberWallpaperTintColor(context, enabled = effectiveUseGradient)

    // Status bar appearance
    val view = LocalView.current
    LaunchedEffect(view, darkTheme) {
        if (view.isInEditMode) return@LaunchedEffect
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Non-dynamic color schemes need manual status bar color
    if (!view.isInEditMode && paintBackground) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = targetBackgroundScheme.background.toArgb()
        }
    }

    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        LocalUseGradient provides effectiveUseGradient,
        LocalHeroOnCards provides themeState.heroOnCards,
        LocalBlurBars provides themeState.blurBars,
        LocalUseEnhancedShading provides themeState.useEnhancedShading,
        LocalReducedMotion provides reducedMotion,
        LocalThemeState provides themeState,
    ) {
        MaterialTheme(
            colorScheme = targetColorScheme,
            shapes = SecondBrainShapes,
            typography = AppTypography,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (paintBackground) {
                    GradientBackground(
                        useGradient = effectiveUseGradient,
                        pageBackground = targetBackgroundScheme.background,
                        gradientBase = targetBackgroundScheme.surface,
                        gradientTop = targetBackgroundScheme.primaryContainer,
                        wallpaperTint = wallpaperTint,
                    )
                }
                content()
            }
        }
    }
}

// ============================================================================
// Color scheme resolution
// ============================================================================

private data class ColorResolution(
    val colorScheme: ColorScheme,
    val backgroundScheme: ColorScheme,
)

@Composable
private fun rememberResolvedColorScheme(
    context: android.content.Context,
    themeState: ThemeState,
    darkTheme: Boolean,
    black: Boolean,
): ColorResolution {
    val dynamicAvailable = themeState.colorSource == ColorSource.MATERIAL_YOU &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val base = if (dynamicAvailable) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val seedColor = parseCustomHex(themeState.customSeedHex)
            ?: colorSourceSeed(themeState.colorSource)
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = darkTheme,
            style = themeState.paletteStyle.toLib(),
            isAmoled = black,
        )
    }

    val oledAdjusted = if (black) base.toOled() else base
    val effectiveShading = if (black) 0.0f else themeState.shadingIntensity
    val tinted = if (effectiveShading > 0.0f) {
        oledAdjusted.tintSurfacesTowardPrimary(darkTheme, effectiveShading)
    } else {
        oledAdjusted
    }
    val themed = if (dynamicAvailable) {
        tinted
    } else {
        tinted
            .boostOutlineForVisibility(darkTheme)
            .boostContainersForSeedThemes(darkTheme)
    }

    return ColorResolution(
        colorScheme = themed,
        backgroundScheme = oledAdjusted,
    )
}

// ============================================================================
// Gradient background
// ============================================================================

@Composable
private fun GradientBackground(
    useGradient: Boolean,
    pageBackground: Color,
    gradientBase: Color,
    gradientTop: Color,
    wallpaperTint: Color?,
) {
    if (useGradient) {
        val gradientBrush = remember(gradientBase, gradientTop, wallpaperTint) {
            val topColor = if (wallpaperTint != null) {
                blendColors(gradientTop, wallpaperTint, wallpaperWeight = 0.28f)
            } else {
                gradientTop
            }
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to topColor.copy(alpha = 0.48f),
                    0.55f to gradientBase.copy(alpha = 0f),
                ),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(gradientBase)
                .background(gradientBrush),
        )
    } else {
        Box(Modifier.fillMaxSize().background(pageBackground))
    }
}

// ============================================================================
// Color transformation helpers
// ============================================================================

/** OLED black mode: dark surfaces become near-black while preserving separation. */
internal fun ColorScheme.toOled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color(0xFF050505),
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF303030),
)

/** Tint surface containers toward the primary accent. */
internal fun ColorScheme.tintSurfacesTowardPrimary(
    dark: Boolean,
    intensityFactor: Float,
): ColorScheme {
    if (intensityFactor <= 0.0f) return this
    val accentArgb = ColorUtils.blendARGB(
        primary.toArgb(),
        primaryContainer.toArgb(),
        if (dark) 0.4f else 0.3f,
    )
    val baseAmount = if (dark) 0.24f else 0.15f
    val amount = baseAmount * intensityFactor

    fun tint(color: Color) = Color(ColorUtils.blendARGB(color.toArgb(), accentArgb, amount))
    return copy(
        surface = tint(surface),
        surfaceVariant = tint(surfaceVariant),
        surfaceDim = tint(surfaceDim),
        surfaceBright = tint(surfaceBright),
        surfaceContainerLowest = tint(surfaceContainerLowest),
        surfaceContainerLow = tint(surfaceContainerLow),
        surfaceContainer = tint(surfaceContainer),
        surfaceContainerHigh = tint(surfaceContainerHigh),
        surfaceContainerHighest = tint(surfaceContainerHighest),
    )
}

/** Boost outline visibility against the background. */
internal fun ColorScheme.boostOutlineForVisibility(dark: Boolean): ColorScheme {
    val targetArgb = onSurface.toArgb()
    val outlineBlend = if (dark) 0.32f else 0.28f
    val outlineVariantBlend = if (dark) 0.20f else 0.16f
    return copy(
        outline = Color(ColorUtils.blendARGB(outline.toArgb(), targetArgb, outlineBlend)),
        outlineVariant = Color(ColorUtils.blendARGB(outlineVariant.toArgb(), targetArgb, outlineVariantBlend)),
    )
}

/** Boost container colors for non-dynamic seed themes. */
internal fun ColorScheme.boostContainersForSeedThemes(dark: Boolean): ColorScheme {
    val primaryBlend = if (dark) 0.30f else 0.24f
    val secondaryBlend = if (dark) 0.26f else 0.20f
    val tertiaryBlend = if (dark) 0.28f else 0.22f
    return copy(
        primaryContainer = Color(ColorUtils.blendARGB(primaryContainer.toArgb(), primary.toArgb(), primaryBlend)),
        secondaryContainer = Color(ColorUtils.blendARGB(secondaryContainer.toArgb(), secondary.toArgb(), secondaryBlend)),
        tertiaryContainer = Color(ColorUtils.blendARGB(tertiaryContainer.toArgb(), tertiary.toArgb(), tertiaryBlend)),
    )
}

private fun blendColors(
    base: Color,
    wallpaper: Color,
    wallpaperWeight: Float,
): Color {
    val clampedWeight = wallpaperWeight.coerceIn(0f, 1f)
    val baseWeight = 1f - clampedWeight
    return Color(
        red = base.red * baseWeight + wallpaper.red * clampedWeight,
        green = base.green * baseWeight + wallpaper.green * clampedWeight,
        blue = base.blue * baseWeight + wallpaper.blue * clampedWeight,
        alpha = base.alpha,
    )
}

private fun parseCustomHex(hex: String): Color? {
    if (hex.isBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}

// ============================================================================
// Reduced motion observer
// ============================================================================

@Composable
private fun rememberReducedMotionEnabled(context: android.content.Context): Boolean {
    val contentResolver: ContentResolver = context.contentResolver

    fun readReducedMotion(): Boolean {
        val animationScale = Settings.Global.getFloat(
            contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return animationScale == 0f
    }

    var reducedMotion by remember(contentResolver) { mutableStateOf(readReducedMotion()) }
    DisposableEffect(contentResolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reducedMotion = readReducedMotion()
            }
        }
        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { contentResolver.unregisterContentObserver(observer) }
    }
    return reducedMotion
}

// ============================================================================
// Wallpaper tint observer
// ============================================================================

@Composable
private fun rememberWallpaperTintColor(
    context: android.content.Context,
    enabled: Boolean,
): Color? {
    if (!enabled) return null
    val applicationContext = context.applicationContext
    val wallpaperManager = remember(applicationContext) {
        WallpaperManager.getInstance(applicationContext)
    }

    fun readWallpaperTint(): Color? {
        val colors = runCatching {
            wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        }.getOrNull()
        return colors?.let { Color(it.primaryColor.toArgb()) }
    }

    var wallpaperTint by remember(wallpaperManager) { mutableStateOf(readWallpaperTint()) }
    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                wallpaperTint = colors?.let { Color(it.primaryColor.toArgb()) }
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { wallpaperManager.removeOnColorsChangedListener(listener) }
    }
    return wallpaperTint
}
