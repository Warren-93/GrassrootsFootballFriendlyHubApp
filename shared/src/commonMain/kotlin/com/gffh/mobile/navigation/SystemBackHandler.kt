package com.gffh.mobile.navigation

import androidx.compose.runtime.Composable

/**
 * Android's system Back (button or gesture): calls [onBack] while [enabled],
 * and leaves Back to the system (closing the app) otherwise. An expect/actual
 * rather than Compose Multiplatform's own BackHandler, which is still
 * experimental in 1.10. A no-op on iOS, which has no system Back.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
