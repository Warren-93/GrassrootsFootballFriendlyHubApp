package com.gffh.mobile.navigation

import androidx.compose.runtime.Composable

/**
 * The system's "back a screen": Android's Back button or gesture, and iOS's
 * swipe in from the left edge. Calls [onBack] while [enabled], and leaves
 * Back to the system (on Android, closing the app) otherwise. Android uses
 * androidx.activity's BackHandler; iOS uses Compose Multiplatform's own,
 * which only exists there as an experimental (and already deprecated) API.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
