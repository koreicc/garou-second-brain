package com.secondbrain.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val COLOR_SOURCE = stringPreferencesKey("color_source")
        val CUSTOM_SEED_HEX = stringPreferencesKey("custom_seed_hex")
        val PALETTE_STYLE = stringPreferencesKey("palette_style")
        val USE_GRADIENT = booleanPreferencesKey("use_gradient")
        val USE_BLACK_THEME = booleanPreferencesKey("use_black_theme")
        val SHADING_INTENSITY = floatPreferencesKey("shading_intensity")
    }

    suspend fun loadSettings(): SavedSettings {
        val prefs = context.dataStore.data.first()
        return SavedSettings(
            serverUrl = prefs[Keys.SERVER_URL] ?: "",
            darkMode = prefs[Keys.DARK_MODE] ?: "SYSTEM",
            colorSource = prefs[Keys.COLOR_SOURCE] ?: "MATERIAL_YOU",
            customSeedHex = prefs[Keys.CUSTOM_SEED_HEX] ?: "",
            paletteStyle = prefs[Keys.PALETTE_STYLE] ?: "TONAL_SPOT",
            useGradient = prefs[Keys.USE_GRADIENT] ?: true,
            useBlackTheme = prefs[Keys.USE_BLACK_THEME] ?: false,
            shadingIntensity = prefs[Keys.SHADING_INTENSITY] ?: 0.0f
        )
    }

    suspend fun saveSettings(settings: SavedSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = settings.serverUrl
            prefs[Keys.DARK_MODE] = settings.darkMode
            prefs[Keys.COLOR_SOURCE] = settings.colorSource
            prefs[Keys.CUSTOM_SEED_HEX] = settings.customSeedHex
            prefs[Keys.PALETTE_STYLE] = settings.paletteStyle
            prefs[Keys.USE_GRADIENT] = settings.useGradient
            prefs[Keys.USE_BLACK_THEME] = settings.useBlackTheme
            prefs[Keys.SHADING_INTENSITY] = settings.shadingIntensity
        }
    }
}

data class SavedSettings(
    val serverUrl: String = "",
    val darkMode: String = "SYSTEM",
    val colorSource: String = "MATERIAL_YOU",
    val customSeedHex: String = "",
    val paletteStyle: String = "TONAL_SPOT",
    val useGradient: Boolean = true,
    val useBlackTheme: Boolean = false,
    val shadingIntensity: Float = 0.0f
) {
    fun toDarkModeOption(): DarkModeOption = when (darkMode) {
        "LIGHT" -> DarkModeOption.LIGHT
        "DARK" -> DarkModeOption.DARK
        else -> DarkModeOption.SYSTEM
    }

    fun toColorSource(): ColorSource = try {
        ColorSource.valueOf(colorSource)
    } catch (_: Exception) {
        ColorSource.MATERIAL_YOU
    }

    fun toPaletteStyle(): PaletteStyleOpt = try {
        PaletteStyleOpt.valueOf(paletteStyle)
    } catch (_: Exception) {
        PaletteStyleOpt.TONAL_SPOT
    }

    fun toThemeState(): ThemeState {
        val darkTheme = when (toDarkModeOption()) {
            DarkModeOption.LIGHT -> false
            DarkModeOption.DARK -> true
            DarkModeOption.SYSTEM -> null
        }
        return ThemeState(
            darkTheme = darkTheme,
            colorSource = toColorSource(),
            customSeedHex = customSeedHex,
            paletteStyle = toPaletteStyle(),
            useGradient = useGradient,
            useBlackTheme = useBlackTheme,
            shadingIntensity = shadingIntensity
        )
    }
}
