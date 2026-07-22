package com.secondbrain.ui.settings

import com.secondbrain.data.DarkModeOption
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt
import com.secondbrain.ui.theme.colorSourceLabel
import com.secondbrain.ui.theme.paletteStyleLabel
import com.secondbrain.ui.theme.transparentTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saveMessage) {
        state.saveMessage?.let {
            kotlinx.coroutines.delay(2000)
            viewModel.onEvent(SettingsEvent.ClearSaveMessage)
        }
    }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(state.saveMessage) {
        state.saveMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(SettingsEvent.ClearSaveMessage)
        }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = transparentTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Server Configuration
            SettingsSection(title = "Server Configuration") {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = { viewModel.onEvent(SettingsEvent.UpdateServerUrl(it)) },
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Connect to your Second Brain backend server. The app communicates with the Go backend through this URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Section 2: Appearance
            SettingsSection(title = "Appearance") {
                // Dark Mode
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DarkModeOption.entries.forEach { option ->
                        val label = when (option) {
                            DarkModeOption.SYSTEM -> "System"
                            DarkModeOption.LIGHT -> "Light"
                            DarkModeOption.DARK -> "Dark"
                        }
                        FilterChip(
                            selected = state.darkMode == option,
                            onClick = { viewModel.onEvent(SettingsEvent.SetDarkMode(option)) },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Color Source
                Text(
                    text = "Color Source",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ColorSource.entries.forEach { source ->
                        FilterChip(
                            selected = state.colorSource == source,
                            onClick = { viewModel.onEvent(SettingsEvent.SetColorSource(source)) },
                            label = { Text(colorSourceLabel(source), style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                // Custom seed hex (visible only when CUSTOM is selected)
                if (state.colorSource == ColorSource.CUSTOM) {
                    OutlinedTextField(
                        value = state.customSeedHex,
                        onValueChange = { viewModel.onEvent(SettingsEvent.UpdateCustomSeedHex(it)) },
                        label = { Text("Custom Seed Color") },
                        placeholder = { Text("#RRGGBB") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Palette Style
                Text(
                    text = "Palette Style",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaletteStyleOpt.entries.forEach { style ->
                        FilterChip(
                            selected = state.paletteStyle == style,
                            onClick = { viewModel.onEvent(SettingsEvent.SetPaletteStyle(style)) },
                            label = { Text(paletteStyleLabel(style), style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Gradient toggle
                SettingsToggleRow(
                    label = "Gradient Background",
                    subtitle = "Use gradient backgrounds on cards and surfaces",
                    checked = state.useGradient,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleGradient) }
                )

                // OLED Black Theme toggle
                SettingsToggleRow(
                    label = "OLED Black Theme",
                    subtitle = "Use true black backgrounds for dark mode (saves battery on OLED screens)",
                    checked = state.useBlackTheme,
                    onCheckedChange = { viewModel.onEvent(SettingsEvent.ToggleBlackTheme) }
                )

                // Shading Intensity slider
                Text(
                    text = "Shading Intensity",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Controls how much surface tinting is applied to cards and containers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = state.shadingIntensity,
                    onValueChange = { viewModel.onEvent(SettingsEvent.SetShadingIntensity(it)) },
                    valueRange = 0f..1f,
                    steps = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(state.shadingIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Full",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 3: About
            SettingsSection(title = "About") {
                AboutRow(label = "App Name", value = "Second Brain")
                AboutRow(label = "Version", value = state.appVersion)
            }

            // Save button
            FilledTonalButton(
                onClick = { viewModel.onEvent(SettingsEvent.SaveSettings) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isSaving) "Saving..." else "Save Settings")
            }

            // Bottom spacing so content isnt clipped by nav bar
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Reusable section card
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

// ---------------------------------------------------------------------------
// Toggle row (label, optional subtitle, switch)
// ---------------------------------------------------------------------------

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// ---------------------------------------------------------------------------
// About info row (label / value pair)
// ---------------------------------------------------------------------------

@Composable
private fun AboutRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
