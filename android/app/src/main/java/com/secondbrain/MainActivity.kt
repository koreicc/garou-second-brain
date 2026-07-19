package com.secondbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.secondbrain.ui.navigation.AppNavigation
import com.secondbrain.ui.theme.SecondBrainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecondBrainTheme {
                AppNavigation()
            }
        }
    }
}
