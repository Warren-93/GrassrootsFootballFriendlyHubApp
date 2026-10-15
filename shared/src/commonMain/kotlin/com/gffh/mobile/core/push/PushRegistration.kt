package com.gffh.mobile.core.push

import com.gffh.mobile.core.auth.TokenStore
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.repository.PushTokenRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Keeps the API's list of this device's push tokens in step with who's
 * signed in:
 *
 * - [keepRegistered] sends the token (`POST /api/v1/me/push-tokens`)
 *   whenever there's both a device token and a signed-in session: on every
 *   app start, as the API asks (it's idempotent), after sign-in, and after
 *   the platform rotates the token. A rejected attempt is tried again when
 *   the session next changes - a stored access token can be stale until
 *   SplashScreen refreshes it.
 * - [unregister] deletes it (`DELETE /api/v1/me/push-tokens`) on sign-out,
 *   while the access token still works, so whoever signs in next on this
 *   phone doesn't get the previous account's notifications.
 *
 * The last token the API accepted is kept in [settings], so sign-out can
 * still remove it when the platform hasn't handed this run's token over yet.
 */
class PushRegistration(
    private val pushTokenRepository: PushTokenRepository,
    private val tokenStore: TokenStore,
    private val settings: Settings
) {

    /** The user and token the API last accepted this run, so a token refresh doesn't send it again. */
    private var registered: Pair<String, DeviceToken>? = null

    /** Runs until cancelled: App() launches it once for the life of the UI. */
    suspend fun keepRegistered() {
        combine(PushTokens.token, tokenStore.session) { device, session -> device to session }
            .collect { (device, session) ->
                if (session == null) {
                    registered = null
                    return@collect
                }
                if (device == null) return@collect
                val key = session.userId to device
                if (registered == key) return@collect
                if (pushTokenRepository.register(device.platform.name, device.token) is ApiResult.Success) {
                    registered = key
                    settings.putString(KEY_REGISTERED_TOKEN, device.token)
                }
            }
    }

    /**
     * Called by AuthRepository.signOut before it clears the session. Gives up
     * after a few seconds, so signing out never hangs on a bad connection.
     *
     * The access token only lasts an hour and nothing renews it while the app
     * stays open, so a 401 is tried once more with [freshAccessToken] - left
     * there, this phone would go on getting the signed-out account's
     * notifications. That token is used for the DELETE alone, not stored: the
     * session is about to be dropped, and storing it would hand
     * [keepRegistered] a "new" session to register the device for again.
     */
    suspend fun unregister(freshAccessToken: suspend () -> String? = { null }) {
        val tokens = listOfNotNull(PushTokens.token.value?.token, settings.getStringOrNull(KEY_REGISTERED_TOKEN)).distinct()
        registered = null
        settings.remove(KEY_REGISTERED_TOKEN)
        // Nothing to authenticate with (the session already lapsed): the API can't be told.
        if (tokenStore.currentAccessToken() == null) return
        var renewed: String? = null
        for (token in tokens) {
            withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                val result = pushTokenRepository.unregister(token, renewed)
                if (result is ApiResult.Failure && result.httpStatus == 401 && renewed == null) {
                    renewed = freshAccessToken()
                    renewed?.let { pushTokenRepository.unregister(token, it) }
                }
            }
        }
    }

    private companion object {
        const val KEY_REGISTERED_TOKEN = "push_registered_token"
        const val UNREGISTER_TIMEOUT_MS = 5_000L
    }
}
