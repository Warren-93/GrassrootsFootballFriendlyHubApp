package com.gffh.mobile.core.push

import androidx.compose.runtime.Composable

/**
 * Asks the platform for whatever it still needs before a notification can be
 * shown, once someone is signed in - [signedIn] - and only when this build
 * can receive push at all ([PushTokens.available]).
 *
 * Android 13 and later need the POST_NOTIFICATIONS runtime permission, asked
 * for here rather than at launch so the prompt lands when notifications
 * about real fixtures are about to start. iOS asks at launch from the app
 * delegate instead (iosApp/iosApp/iOSApp.swift), so there the actual does
 * nothing. Emits no UI either way.
 */
@Composable
expect fun PushNotificationSetup(signedIn: Boolean)
