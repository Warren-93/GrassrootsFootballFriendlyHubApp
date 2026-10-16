package com.gffh.mobile.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gffh.mobile.core.push.NotificationTap
import com.gffh.mobile.core.push.PushPlatform
import com.gffh.mobile.core.push.PushTokens

/**
 * Receives Firebase Cloud Messaging pushes (the API's FcmSender sends
 * `notification` + `data`, see the shared API contract section 6).
 *
 * While the app is in the background Android draws the notification itself,
 * on the channel and with the icon the manifest names - the same ones used
 * here - and a tap starts MainActivity with the push's data as intent extras.
 * In the foreground the payload comes here instead, so this builds that same
 * notification, on the one "Kickabout" channel, with the same extras on the
 * intent that opens MainActivity, which hands them to NotificationTaps.
 */
class KickaboutMessagingService : FirebaseMessagingService() {

    /** The token rotates occasionally; PushRegistration sends the new one to the API. */
    override fun onNewToken(token: String) {
        PushTokens.update(PushPlatform.ANDROID, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: "Kickabout"
        val body = notification?.body ?: message.data["body"] ?: return
        show(title, body, message.data)
    }

    private fun show(title: String, body: String, data: Map<String, String>) {
        if (!canShowNotifications()) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        createChannel(this)

        // Keyed on the text and the ids it's about, so the same notification
        // arriving twice replaces itself instead of stacking, while different
        // ones sit side by side - even two worded alike, such as the same team
        // confirming two friendlies.
        val notificationId = (listOf(body) + NotificationTap.KEYS.map { data[it] }).hashCode()

        // The ids the API sent (NotificationTap.KEYS), as the extras FCM itself
        // puts on the launch intent of a notification it draws.
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            NotificationTap.KEYS.forEach { key -> data[key]?.let { putExtra(key, it) } }
        }
        // Extras don't make one PendingIntent different from another, the
        // request code does: one per notification, so each keeps its own ids
        // rather than every notification opening the latest one's screen.
        // FLAG_IMMUTABLE: nothing is added to the intent later, and Android 12+ requires one of the two.
        val contentIntent = PendingIntent.getActivity(
            this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        }
        val built = builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        manager.notify(notificationId, built)
    }

    /** Android 13+ shows nothing without POST_NOTIFICATIONS, which the app asks for after sign-in. */
    private fun canShowNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Also named in the manifest as Firebase's default channel - keep the two the same. */
        const val CHANNEL_ID = "kickabout"
        private const val CHANNEL_NAME = "Kickabout"

        /**
         * Safe to call any number of times: creating a channel that already
         * exists only refreshes its name and description, never the user's
         * own settings for it.
         */
        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Friendly requests, fixtures, messages and verification updates."
                }
            )
        }
    }
}
