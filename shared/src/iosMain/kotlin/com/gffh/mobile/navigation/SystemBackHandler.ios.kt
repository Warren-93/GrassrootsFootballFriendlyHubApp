package com.gffh.mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler

/**
 * iOS's swipe in from the left edge, which Compose Multiplatform's
 * BackHandler hears from the app's UIViewController - the same "back a
 * screen" that Android's Back button and gesture give. Without it, screens
 * with no Back button of their own (Calendar, Team profile, the invitation
 * composer, ...) could only be left through the bottom tabs.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("DEPRECATION")
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
