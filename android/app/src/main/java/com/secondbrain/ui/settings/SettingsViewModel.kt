package com.secondbrain.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.DarkModeOption
import com.secondbrain.data.SavedSettings
import com.secondbrain.data.SettingsPreferences
import com.secondbrain.data.ThemeState
import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    // Server
    val serverUrl: String = "",
    // Theme
    val darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    val colorSource: ColorSource = ColorSource.MATERIAL_YOU,
    val customSeedHex: String = "",
    val paletteStyle: PaletteStyleOpt = PaletteStyleOpt.TONAL_SPOT,
    val useGradient: Boolean = true,
    val useBlackTheme: Boolean = false,
    val shadingIntensity: Float = 0.0f,
    val appVersion: String = "1.0.0",
    // Persistence state
    val isSaving: Boolean = false,
    val saveMessage: String? = null
)

sealed interface SettingsEvent {
    data class UpdateServerUrl(val url: String) : SettingsEvent
    data class SetDarkMode(val option: DarkModeOption) : SettingsEvent
    data class SetColorSource(val source: ColorSource) : SettingsEvent
    data class UpdateCustomSeedHex(val hex: String) : SettingsEvent
    data class SetPaletteStyle(val style: PaletteStyleOpt) : SettingsEvent
    data object ToggleGradient : SettingsEvent
    data object ToggleBlackTheme : SettingsEvent
    data class SetShadingIntensity(val intensity: Float) : SettingsEvent
    data object SaveSettings : SettingsEvent
    data object ClearSaveMessage : SettingsEvent
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = SettingsPreferences(application)

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateServerUrl -> _state.update { it.copy(serverUrl = event.url) }
            is SettingsEvent.SetDarkMode -> {
                _state.update { it.copy(darkMode = event.option) }
                updateThemeState()
            }
            is SettingsEvent.SetColorSource -> {
                _state.update { it.copy(colorSource = event.source) }
                updateThemeState()
            }
            is SettingsEvent.UpdateCustomSeedHex -> {
                _state.update { it.copy(customSeedHex = event.hex) }
                updateThemeState()
            }
            is SettingsEvent.SetPaletteStyle -> {
                _state.update { it.copy(paletteStyle = event.style) }
                updateThemeState()
            }
            is SettingsEvent.ToggleGradient -> {
                _state.update { it.copy(useGradient = !it.useGradient) }
                updateThemeState()
            }
            is SettingsEvent.ToggleBlackTheme -> {
                _state.update { it.copy(useBlackTheme = !it.useBlackTheme) }
                updateThemeState()
            }
            is SettingsEvent.SetShadingIntensity -> {
                _state.update { it.copy(shadingIntensity = event.intensity) }
                updateThemeState()
            }
            is SettingsEvent.SaveSettings -> saveSettings()
            is SettingsEvent.ClearSaveMessage -> _state.update { it.copy(saveMessage = null) }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val saved = preferences.loadSettings()
            _state.update {
                it.copy(
                    serverUrl = saved.serverUrl,
                    darkMode = saved.toDarkModeOption(),
                    colorSource = saved.toColorSource(),
                    customSeedHex = saved.customSeedHex,
                    paletteStyle = saved.toPaletteStyle(),
                    useGradient = saved.useGradient,
                    useBlackTheme = saved.useBlackTheme,
                    shadingIntensity = saved.shadingIntensity
                )
            }
            _themeState.value = saved.toThemeState()
        }
    }

    private fun updateThemeState() {
        val s = _state.value
        val darkTheme = when (s.darkMode) {
            DarkModeOption.LIGHT -> false
            DarkModeOption.DARK -> true
            DarkModeOption.SYSTEM -> null
        }
        _themeState.value = ThemeState(
            darkTheme = darkTheme,
            colorSource = s.colorSource,
            customSeedHex = s.customSeedHex,
            paletteStyle = s.paletteStyle,
            useGradient = s.useGradient,
            useBlackTheme = s.useBlackTheme,
            shadingIntensity = s.shadingIntensity
        )
    }

    private fun saveSettings() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val saved = SavedSettings(
                serverUrl = s.serverUrl,
                darkMode = s.darkMode.name,
                colorSource = s.colorSource.name,
                customSeedHex = s.customSeedHex,
                paletteStyle = s.paletteStyle.name,
                useGradient = s.useGradient,
                useBlackTheme = s.useBlackTheme,
                shadingIntensity = s.shadingIntensity
            )
            preferences.saveSettings(saved)
            _state.update { it.copy(isSaving = false, saveMessage = "Settings saved") }
        }
    }
}
