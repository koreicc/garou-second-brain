package com.secondbrain.data

import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt

/**
 * Runtime theme state that drives the dynamic palette system.
 * Inspired by Remember/FilePipe's ThemeState pattern.
 *
 * Default values match the app's original static blue theme.
 */
data class ThemeState(
    /** Whether dark mode is forced (null = follow system). */
    val darkTheme: Boolean? = null,
    /** Color source for the dynamic palette. */
    val colorSource: ColorSource = ColorSource.DEFAULT,
    /** Custom hex seed color (used when colorSource == CUSTOM). */
    val customSeedHex: String = "",
    /** Palette style algorithm. */
    val paletteStyle: PaletteStyleOpt = PaletteStyleOpt.TONAL_SPOT,
    /** Whether to use gradient background. */
    val useGradient: Boolean = true,
    /** Whether to use OLED black theme. */
    val useBlackTheme: Boolean = false,
    /** Surface tinting intensity (0.0 = none, 1.0 = full). */
    val shadingIntensity: Float = 0.0f,
    /** Whether hero images show on cards. */
    val heroOnCards: Boolean = false,
    /** Whether frosted-glass blur bars are enabled. */
    val blurBars: Boolean = true,
) {
    val useEnhancedShading: Boolean
        get() = shadingIntensity > 0.0f

    fun effectiveDarkTheme(systemDark: Boolean): Boolean =
        darkTheme ?: systemDark
}
