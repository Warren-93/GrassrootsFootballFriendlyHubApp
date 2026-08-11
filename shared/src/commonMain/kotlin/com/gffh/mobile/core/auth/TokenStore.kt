package com.gffh.mobile.core.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class StoredSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val displayName: String,
    val emailVerified: Boolean
)

/**
 * Persists the session client-side. `multiplatform-settings` backs onto
 * SharedPreferences on Android and NSUserDefaults on iOS - adequate for this
 * MVP; a hardened build would move the refresh token into the platform
 * keystore/keychain specifically, since it is the longer-lived credential.
 */
class TokenStore(private val settings: Settings) {

    private val _session = MutableStateFlow(loadSession())
    val session: StateFlow<StoredSession?> = _session

    private fun loadSession(): StoredSession? {
        val token = settings.get<String>(KEY_TOKEN) ?: return null
        val refreshToken = settings.get<String>(KEY_REFRESH_TOKEN) ?: return null
        val userId = settings.get<String>(KEY_USER_ID) ?: return null
        val email = settings.get<String>(KEY_EMAIL) ?: return null
        val displayName = settings.get<String>(KEY_DISPLAY_NAME) ?: return null
        val emailVerified = settings.get<Boolean>(KEY_EMAIL_VERIFIED) ?: false
        return StoredSession(token, refreshToken, userId, email, displayName, emailVerified)
    }

    fun save(session: StoredSession) {
        settings.putString(KEY_TOKEN, session.accessToken)
        settings.putString(KEY_REFRESH_TOKEN, session.refreshToken)
        settings.putString(KEY_USER_ID, session.userId)
        settings.putString(KEY_EMAIL, session.email)
        settings.putString(KEY_DISPLAY_NAME, session.displayName)
        settings.putBoolean(KEY_EMAIL_VERIFIED, session.emailVerified)
        _session.value = session
    }

    /** Swaps only the token pair after a refresh, keeping the rest of the session. */
    fun updateTokens(accessToken: String, refreshToken: String) {
        val current = _session.value ?: return
        save(current.copy(accessToken = accessToken, refreshToken = refreshToken))
    }

    fun updateEmailVerified(verified: Boolean) {
        val current = _session.value ?: return
        save(current.copy(emailVerified = verified))
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_DISPLAY_NAME)
        settings.remove(KEY_EMAIL_VERIFIED)
        _session.value = null
    }

    fun currentAccessToken(): String? = _session.value?.accessToken
    fun currentRefreshToken(): String? = _session.value?.refreshToken

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_EMAIL_VERIFIED = "email_verified"
    }
}
