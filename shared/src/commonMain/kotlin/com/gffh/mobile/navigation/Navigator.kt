package com.gffh.mobile.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * A deliberately simple state-based back stack rather than a full navigation
 * library: this app's routes are a flat set of pushes with occasional resets
 * (e.g. sign-out clears back to Welcome), which a list handles without the
 * dependency-version risk of a multiplatform nav-compose artifact.
 */
class Navigator(start: Route) {
    private val backStack = mutableStateOf(listOf(start))

    val current: Route get() = backStack.value.last()
    val canGoBack: Boolean get() = backStack.value.size > 1

    fun push(route: Route) {
        backStack.value = backStack.value + route
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        backStack.value = backStack.value.dropLast(1)
        return true
    }

    /** Swaps the current top of the stack for [route] - used when a screen redirects on load (e.g. a search-then-results hop) so back doesn't return to the intermediate screen. */
    fun replace(route: Route) {
        backStack.value = backStack.value.dropLast(1) + route
    }

    /** Replaces the whole stack - used for post-login/sign-out resets where back should not return to auth screens. */
    fun resetTo(route: Route) {
        backStack.value = listOf(route)
    }
}
