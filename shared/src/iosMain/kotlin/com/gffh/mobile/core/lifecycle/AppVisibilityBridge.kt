package com.gffh.mobile.core.lifecycle

/**
 * What iosApp's AppDelegate calls when the app comes to the foreground or
 * goes to the background (Swift: `AppVisibilityBridgeKt.markAppForeground()`,
 * `AppVisibilityBridgeKt.markAppBackground()`). See [AppVisibility].
 */
fun markAppForeground() {
    AppVisibility.setInForeground(true)
}

fun markAppBackground() {
    AppVisibility.setInForeground(false)
}
