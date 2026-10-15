package com.gffh.mobile.core.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The API's names: an FCM registration token is ANDROID, an APNs device token is IOS. */
enum class PushPlatform { ANDROID, IOS }

data class DeviceToken(val platform: PushPlatform, val token: String)

/**
 * Where the platform hands over this device's push token. It arrives
 * asynchronously and outside any screen - from Firebase on Android, from the
 * AppDelegate on iOS, often before [com.gffh.mobile.App] is even composed -
 * so it lives in one process-wide place that [PushRegistration] watches.
 */
object PushTokens {

    private val _available = MutableStateFlow(false)
    private val _token = MutableStateFlow<DeviceToken?>(null)

    /**
     * True once this build can receive push at all. On Android that means
     * Firebase was configured at build time; without it the app never asks
     * for notification permission, since nothing could arrive.
     */
    val available: StateFlow<Boolean> = _available

    /** The latest token, or null until the platform has handed one over. */
    val token: StateFlow<DeviceToken?> = _token

    fun markAvailable() {
        _available.value = true
    }

    /** Called with every token the platform hands over, including a rotated one. */
    fun update(platform: PushPlatform, token: String) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) return
        _available.value = true
        _token.value = DeviceToken(platform, trimmed)
    }
}
