package com.secondbrain.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

/**
 * Seed color sources for the dynamic palette system.
 * Inspired by the Remember/FilePipe color source system.
 */
enum class ColorSource {
    /** Android wallpaper-based colors (Material You, API 31+). */
    MATERIAL_YOU,
    /** Default seed color. */
    DEFAULT,
    /** Custom hex color input. */
    CUSTOM,
    /** Hand-tuned ember orange. */
    CURATED_EMBER,
    /** Hand-tuned grove green. */
    CURATED_GROVE,
    /** Hand-tuned honey yellow. */
    CURATED_HONEY,
    /** Hand-tuned ocean blue. */
    CURATED_OCEAN,
    /** Hand-tuned iris purple. */
    CURATED_IRIS,
    /** Hand-tuned dusk gray. */
    CURATED_DUSK,
    /** Hand-tuned berry pink. */
    CURATED_BERRY,
}

/**
 * Palette style mapping to materialkolor's PaletteStyle.
 */
enum class PaletteStyleOpt {
    TONAL_SPOT,
    NEUTRAL,
    VIBRANT,
    EXPRESSIVE,
    RAINBOW,
    FRUIT_SALAD,
    MONOCHROME,
    FIDELITY,
    CONTENT,
}

fun PaletteStyleOpt.toLib(): PaletteStyle =
    when (this) {
        PaletteStyleOpt.TONAL_SPOT -> PaletteStyle.TonalSpot
        PaletteStyleOpt.NEUTRAL -> PaletteStyle.Neutral
        PaletteStyleOpt.VIBRANT -> PaletteStyle.Vibrant
        PaletteStyleOpt.EXPRESSIVE -> PaletteStyle.Expressive
        PaletteStyleOpt.RAINBOW -> PaletteStyle.Rainbow
        PaletteStyleOpt.FRUIT_SALAD -> PaletteStyle.FruitSalad
        PaletteStyleOpt.MONOCHROME -> PaletteStyle.Monochrome
        PaletteStyleOpt.FIDELITY -> PaletteStyle.Fidelity
        PaletteStyleOpt.CONTENT -> PaletteStyle.Content
    }

/**
 * A hand-tuned primary/secondary/tertiary color triplet.
 */
data class CuratedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
)

/** Seed color for each ColorSource (used when Material You is unavailable). */
fun colorSourceSeed(source: ColorSource): Color =
    when (source) {
        ColorSource.MATERIAL_YOU -> DefaultSeed
        ColorSource.DEFAULT -> DefaultSeed
        ColorSource.CUSTOM -> DefaultSeed
        ColorSource.CURATED_EMBER -> SeedEmber
        ColorSource.CURATED_GROVE -> SeedGrove
        ColorSource.CURATED_HONEY -> SeedHoney
        ColorSource.CURATED_OCEAN -> SeedOcean
        ColorSource.CURATED_IRIS -> SeedIris
        ColorSource.CURATED_DUSK -> SeedDusk
        ColorSource.CURATED_BERRY -> SeedBerry
    }

/** Display label for each ColorSource. */
fun colorSourceLabel(source: ColorSource): String =
    when (source) {
        ColorSource.MATERIAL_YOU -> "Material You"
        ColorSource.DEFAULT -> "Default"
        ColorSource.CUSTOM -> "Custom"
        ColorSource.CURATED_EMBER -> "Ember"
        ColorSource.CURATED_GROVE -> "Grove"
        ColorSource.CURATED_HONEY -> "Honey"
        ColorSource.CURATED_OCEAN -> "Ocean"
        ColorSource.CURATED_IRIS -> "Iris"
        ColorSource.CURATED_DUSK -> "Dusk"
        ColorSource.CURATED_BERRY -> "Berry"
    }
