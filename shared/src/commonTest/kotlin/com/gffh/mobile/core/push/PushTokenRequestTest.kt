package com.gffh.mobile.core.push

import com.gffh.mobile.model.DeletePushTokenRequest
import com.gffh.mobile.model.RegisterPushTokenRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** The API reads `platform` as its own PushPlatform enum, so the names have to match exactly. */
class PushTokenRequestTest {

    // Same settings as ApiClient's.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun aRegistrationNamesThePlatformTheWayTheApiSpellsIt() {
        val body = json.encodeToString(
            RegisterPushTokenRequest.serializer(),
            RegisterPushTokenRequest(PushPlatform.ANDROID.name, "fcm-token")
        )

        assertEquals("""{"platform":"ANDROID","token":"fcm-token"}""", body)
    }

    @Test
    fun anIosRegistrationSendsTheHexDeviceToken() {
        val body = json.encodeToString(
            RegisterPushTokenRequest.serializer(),
            RegisterPushTokenRequest(PushPlatform.IOS.name, "a1b2c3")
        )

        assertEquals("""{"platform":"IOS","token":"a1b2c3"}""", body)
    }

    @Test
    fun signOutSendsOnlyTheToken() {
        val body = json.encodeToString(DeletePushTokenRequest.serializer(), DeletePushTokenRequest("a1b2c3"))

        assertEquals("""{"token":"a1b2c3"}""", body)
    }

    // PushTokens is process-wide (the platform hands tokens over outside any
    // screen), so these assert on changes rather than on a starting value.

    @Test
    fun aTokenIsStoredTrimmed() {
        PushTokens.update(PushPlatform.IOS, " a1b2c3\n")

        assertEquals(DeviceToken(PushPlatform.IOS, "a1b2c3"), PushTokens.token.value)
    }

    @Test
    fun aBlankTokenIsIgnoredRatherThanStored() {
        PushTokens.update(PushPlatform.ANDROID, "fcm-token")
        PushTokens.update(PushPlatform.ANDROID, "   ")

        assertEquals(DeviceToken(PushPlatform.ANDROID, "fcm-token"), PushTokens.token.value)
    }
}
