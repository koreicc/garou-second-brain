package com.secondbrain.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Calls [onResume] every time the composable's lifecycle enters the RESUMED state.
 *
 * Use this to trigger data reloading when navigating back from another screen.
 * Unlike [LaunchedEffect][androidx.compose.runtime.LaunchedEffect] which runs only once
 * on first composition, [RefreshOnResume] runs every time the screen becomes visible
 * again (e.g., pressing back from a detail/edit screen).
 *
 * @param keys Optional keys that, when changed, will re-register the lifecycle observer.
 * @param onResume The block to execute on every RESUME lifecycle event.
 */
@Composable
fun RefreshOnResume(
    vararg keys: Any?,
    onResume: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner, *keys) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
