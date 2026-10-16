package com.gffh.mobile.core.push

import androidx.compose.runtime.Composable

/**
 * Nothing to do: the app delegate (iosApp/iosApp/iOSApp.swift) asks
 * UNUserNotificationCenter for permission at launch and hands the APNs
 * device token to [registerIosPushToken].
 */
@Composable
actual fun PushNotificationSetup(signedIn: Boolean) {
}
