package com.gffh.mobile.core.push

/**
 * What iosApp's AppDelegate calls into (Swift: `PushBridgeKt.markIosPushAvailable()`,
 * `PushBridgeKt.registerIosPushToken(hexToken:)`,
 * `PushBridgeKt.handleNotificationTap(type:requestId:fixtureId:conversationId:teamId:)` -
 * the `shared` framework, the same one ContentView.swift imports for MainViewControllerKt).
 *
 * Registering with the API is [PushRegistration]'s job, and opening a tapped
 * push is App's: these only record what the platform has handed over, since a
 * token usually arrives before anyone is signed in, and a tap that launches
 * the app arrives before there's any UI.
 */

/** Called once the user has allowed notifications, so the app knows push is possible at all. */
fun markIosPushAvailable() {
    PushTokens.markAvailable()
}

/** [hexToken] is the APNs device token as lowercase hex, which is what the API stores for IOS. */
fun registerIosPushToken(hexToken: String) {
    PushTokens.update(PushPlatform.IOS, hexToken)
}

/**
 * Called when someone taps a notification, with the custom keys the API's
 * ApnsSender puts next to `aps` (each nil when the push didn't carry it).
 * App opens the screen it's about - see [NotificationTaps].
 */
fun handleNotificationTap(
    type: String?,
    requestId: String?,
    fixtureId: String?,
    conversationId: String?,
    teamId: String?
) {
    NotificationTaps.handle(
        mapOf(
            "type" to type,
            "requestId" to requestId,
            "fixtureId" to fixtureId,
            "conversationId" to conversationId,
            "teamId" to teamId
        )
    )
}
