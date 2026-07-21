package com.secondbrain.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Material 3 default typography scale.
 * The Bikram Design DNA uses material defaults for all type roles,
 * with optional custom font family support.
 */
val AppTypography = Typography()

/**
 * Swaps only the fontFamily on every Material type role while keeping
 * sizes/line heights from the defaults.
 */
fun customFontTypography(fontFamily: FontFamily): Typography {
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.withFontFamilyOnly(fontFamily),
        displayMedium = defaults.displayMedium.withFontFamilyOnly(fontFamily),
        displaySmall = defaults.displaySmall.withFontFamilyOnly(fontFamily),
        headlineLarge = defaults.headlineLarge.withFontFamilyOnly(fontFamily),
        headlineMedium = defaults.headlineMedium.withFontFamilyOnly(fontFamily),
        headlineSmall = defaults.headlineSmall.withFontFamilyOnly(fontFamily),
        titleLarge = defaults.titleLarge.withFontFamilyOnly(fontFamily),
        titleMedium = defaults.titleMedium.withFontFamilyOnly(fontFamily),
        titleSmall = defaults.titleSmall.withFontFamilyOnly(fontFamily),
        bodyLarge = defaults.bodyLarge.withFontFamilyOnly(fontFamily),
        bodyMedium = defaults.bodyMedium.withFontFamilyOnly(fontFamily),
        bodySmall = defaults.bodySmall.withFontFamilyOnly(fontFamily),
        labelLarge = defaults.labelLarge.withFontFamilyOnly(fontFamily),
        labelMedium = defaults.labelMedium.withFontFamilyOnly(fontFamily),
        labelSmall = defaults.labelSmall.withFontFamilyOnly(fontFamily),
    )
}

private fun TextStyle.withFontFamilyOnly(fontFamily: FontFamily): TextStyle =
    copy(fontFamily = fontFamily)
