package com.gffh.mobile.core.auth

import com.gffh.mobile.model.OAuthSignInRequest
import com.gffh.mobile.model.TokenResponse
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `POST /api/v1/auth/oauth/{google|apple}`'s body, and the newUser flag that decides where the app goes next. */
class SocialSignInRequestTest {

    // Same settings as ApiClient's.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    private val tokenResponse = """
        {"accessToken":"a","refreshToken":"r","tokenType":"Bearer","expiresIn":900,
         "user":{"id":"u1","email":"sam@example.com","displayName":"Sam","emailVerified":true}
    """.trimIndent()

    @Test
    fun theProvidersAreAddressedTheWayTheApiSpellsThem() {
        assertEquals("google", SocialProvider.GOOGLE.path)
        assertEquals("apple", SocialProvider.APPLE.path)
    }

    @Test
    fun aGoogleSignInSendsOnlyTheIdToken() {
        val body = json.encodeToString(OAuthSignInRequest.serializer(), OAuthSignInRequest("google-jwt"))

        assertEquals("""{"idToken":"google-jwt"}""", body)
    }

    @Test
    fun anAppleSignInSendsTheNameAppleHandedOver() {
        val body = json.encodeToString(OAuthSignInRequest.serializer(), OAuthSignInRequest("apple-jwt", "Sam Smith"))

        assertEquals("""{"idToken":"apple-jwt","displayName":"Sam Smith"}""", body)
    }

    @Test
    fun aBrandNewAccountIsFlaggedForOnboarding() {
        val response = json.decodeFromString(TokenResponse.serializer(), "$tokenResponse,\"newUser\":true}")

        assertTrue(response.newUser)
    }

    @Test
    fun aPasswordSignInHasNoNewUserFlagAndCountsAsReturning() {
        val response = json.decodeFromString(TokenResponse.serializer(), "$tokenResponse}")

        assertFalse(response.newUser)
    }
}
