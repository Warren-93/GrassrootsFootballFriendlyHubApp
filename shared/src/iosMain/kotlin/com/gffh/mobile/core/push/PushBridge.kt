package com.gffh.mobile.core.push

/**
 * What iosApp's AppDelegate calls into (Swift: `PushBridgeKt.markIosPushAvailable()`,
 * `PushBridgeKt.registerIosPushToken(hexToken:)` - the `shared` framework, the
 * same one ContentView.swift imports for MainViewControllerKt).
 *
 * Registering with the API is [PushRegistration]'s job: these only record
 * what the platform has handed over, since a token usually arrives before
 * anyone is signed in.
 */

/** Called once the user has allowed notifications, so the app knows push is possible at all. */
fun markIosPushAvailable() {
    PushTokens.markAvailable()
}

/** [hexToken] is the APNs device token as lowercase hex, which is what the API stores for IOS. */
fun registerIosPushToken(hexToken: String) {
    PushTokens.update(PushPlatform.IOS, hexToken)
}
