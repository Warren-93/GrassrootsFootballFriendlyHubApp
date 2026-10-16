package com.gffh.mobile.repository

import com.gffh.mobile.model.NotificationPreferenceView
import com.gffh.mobile.model.UpdateNotificationPreferenceRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class NotificationRepositoryTest {

    // Same settings as ApiClient's.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Test
    fun switchingADeliverySwitchBackOnIsStillSent() {
        val prefs = NotificationPreferenceView(
            friendlyRequests = true, fixtures = true, verification = true, messages = true,
            emailEnabled = true, pushEnabled = false
        )

        val body = json.encodeToString(UpdateNotificationPreferenceRequest.serializer(), prefs.toUpdateRequest())

        assertTrue(body.contains("\"emailEnabled\":true"), body)
        assertTrue(body.contains("\"pushEnabled\":false"), body)
    }
}
