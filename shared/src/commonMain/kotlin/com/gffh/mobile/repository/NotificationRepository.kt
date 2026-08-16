package com.gffh.mobile.repository

import com.gffh.mobile.core.network.ApiClient
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.NotificationPreferenceView
import com.gffh.mobile.model.NotificationView
import com.gffh.mobile.model.UnreadCountView
import io.ktor.client.request.*
import io.ktor.http.HttpMethod

class NotificationRepository(private val api: ApiClient) {

    suspend fun list(): ApiResult<List<NotificationView>> = api.request {
        method = HttpMethod.Get
        url("/api/v1/notifications")
    }

    suspend fun unreadCount(): ApiResult<UnreadCountView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/notifications/unread-count")
    }

    suspend fun markRead(id: String): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/notifications/$id/read")
    }

    suspend fun markAllRead(): ApiResult<Unit> = api.request {
        method = HttpMethod.Post
        url("/api/v1/notifications/read-all")
    }

    suspend fun clearAll(): ApiResult<Unit> = api.request {
        method = HttpMethod.Delete
        url("/api/v1/notifications")
    }

    suspend fun getPreferences(): ApiResult<NotificationPreferenceView> = api.request {
        method = HttpMethod.Get
        url("/api/v1/me/notification-preferences")
    }

    suspend fun updatePreferences(preferences: NotificationPreferenceView): ApiResult<NotificationPreferenceView> = api.request {
        method = HttpMethod.Patch
        url("/api/v1/me/notification-preferences")
        setBody(preferences)
    }
}
