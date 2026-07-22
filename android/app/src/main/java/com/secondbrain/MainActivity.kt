package com.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.secondbrain.ui.navigation.AppNavigation
import com.secondbrain.ui.settings.SettingsViewModel
import com.secondbrain.ui.theme.SecondBrainTheme

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeState by settingsViewModel.themeState.collectAsState()
            SecondBrainTheme(themeState = themeState) {
                AppNavigation()
            }
        }
    }
}
