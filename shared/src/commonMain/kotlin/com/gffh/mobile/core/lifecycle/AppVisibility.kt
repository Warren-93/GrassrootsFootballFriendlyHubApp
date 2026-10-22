package com.gffh.mobile.core.lifecycle

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app is on screen, so screens that poll (the chat) can stop
 * while it's in the background. Android's MainActivity sets it in
 * onStart/onStop; iosApp's AppDelegate through AppVisibilityBridge.kt.
 * Starts true, so a platform that never reports keeps the old behaviour.
 */
object AppVisibility {
    private val _inForeground = MutableStateFlow(true)
    val inForeground: StateFlow<Boolean> = _inForeground.asStateFlow()

    fun setInForeground(inForeground: Boolean) {
        _inForeground.value = inForeground
    }
}
