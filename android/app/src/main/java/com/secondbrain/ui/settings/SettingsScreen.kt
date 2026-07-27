package com.secondbrain.ui.settings

import com.secondbrain.data.DarkModeOption
import com.secondbrain.ui.settings.SettingsViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.secondbrain.ui.theme.ColorSource
import com.secondbrain.ui.theme.PaletteStyleOpt
import com.secondbrain.ui.theme.colorSourceLabel
import com.secondbrain.ui.theme.paletteStyleLabel
import com.secondbrain.ui.common.AnimatedSection
import com.secondbrain.ui.theme.transparentTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
        AnimatedSection {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Server Configuration
            Text(
                text = "Server Configuration",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

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

            HorizontalDivider()

            // Section: Appearance
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            // Dark Mode
            Text(
                text = "Dark Mode",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DarkModeOption.entries.forEachIndexed { index, option ->
                    val label = when (option) {
                        DarkModeOption.SYSTEM -> "System"
                        DarkModeOption.LIGHT -> "Light"
                        DarkModeOption.DARK -> "Dark"
                    }
                    SegmentedButton(
                        selected = state.darkMode == option,
                        onClick = { viewModel.onEvent(SettingsEvent.SetDarkMode(option)) },
                        shape = SegmentedButtonDefaults.itemShape(index, DarkModeOption.entries.size)
                    ) {
                        Text(label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Source
            Text(
                text = "Color Source",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ColorSource.entries.forEachIndexed { index, source ->
                    SegmentedButton(
                        selected = state.colorSource == source,
                        onClick = { viewModel.onEvent(SettingsEvent.SetColorSource(source)) },
                        shape = SegmentedButtonDefaults.itemShape(index, ColorSource.entries.size)
                    ) {
                        Text(colorSourceLabel(source), style = MaterialTheme.typography.bodySmall)
                    }
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

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PaletteStyleOpt.entries.forEachIndexed { index, style ->
                    SegmentedButton(
                        selected = state.paletteStyle == style,
                        onClick = { viewModel.onEvent(SettingsEvent.SetPaletteStyle(style)) },
                        shape = SegmentedButtonDefaults.itemShape(index, PaletteStyleOpt.entries.size)
                    ) {
                        Text(paletteStyleLabel(style), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HorizontalDivider()

            // Section: About
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            AboutRow(label = "App Name", value = "Second Brain")
            AboutRow(label = "Version", value = state.appVersion)

            HorizontalDivider()

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
