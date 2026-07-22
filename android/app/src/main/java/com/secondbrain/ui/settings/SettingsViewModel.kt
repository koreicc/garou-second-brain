package com.secondbrain.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.data.ThemeState
import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

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
    val appVersion: String = "1.0.0"
)

enum class DarkModeOption {
    SYSTEM, LIGHT, DARK
}

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
}

class SettingsViewModel : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.UpdateServerUrl -> _state.update { it.copy(serverUrl = event.url) }
            is SettingsEvent.SetDarkMode -> _state.update { it.copy(darkMode = event.option) }
            is SettingsEvent.SetColorSource -> _state.update { it.copy(colorSource = event.source) }
            is SettingsEvent.UpdateCustomSeedHex -> _state.update { it.copy(customSeedHex = event.hex) }
            is SettingsEvent.SetPaletteStyle -> _state.update { it.copy(paletteStyle = event.style) }
            is SettingsEvent.ToggleGradient -> _state.update { it.copy(useGradient = !it.useGradient) }
            is SettingsEvent.ToggleBlackTheme -> _state.update { it.copy(useBlackTheme = !it.useBlackTheme) }
            is SettingsEvent.SetShadingIntensity -> _state.update { it.copy(shadingIntensity = event.intensity) }
            is SettingsEvent.SaveSettings -> saveSettings()
        }
    }

    private fun loadSettings() {
        // TODO: Load from DataStore when persistence is implemented
        // For now use defaults
    }

    private fun saveSettings() {
        // TODO: Persist to DataStore
        // For now just a no-op that could show a success message
    }
}
