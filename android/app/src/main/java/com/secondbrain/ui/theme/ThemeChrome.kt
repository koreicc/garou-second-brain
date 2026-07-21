package com.secondbrain.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * TopAppBar colors with transparent containers so scroll content or gradient
 * background reads through. Text/icons use onSurface for legibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun transparentTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)

/**
 * Large/expanded TopAppBar with transparent container.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun transparentLargeTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
)
