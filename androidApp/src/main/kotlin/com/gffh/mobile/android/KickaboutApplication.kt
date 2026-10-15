package com.gffh.mobile.android

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.gffh.mobile.core.auth.AndroidAppContext
import com.gffh.mobile.core.push.PushPlatform
import com.gffh.mobile.core.push.PushTokens

/**
 * Runs before anything else in the process - the launcher Activity, and also
 * [KickaboutMessagingService] when a push wakes the app up with no UI - so
 * both find the app context and Firebase ready.
 *
 * Firebase is initialised by hand from BuildConfig rather than from a
 * `google-services.json` and the google-services Gradle plugin: the four
 * identifiers are build properties (see androidApp/build.gradle.kts and the
 * README), so no secret file has to live in the repository. When any of them
 * is blank this build simply has no push: Firebase is never initialised, no
 * token is ever asked for, and nothing asks for notification permission.
 * That's why the manifest removes FirebaseInitProvider - without it, the
 * library would try to initialise itself from resources that don't exist.
 */
class KickaboutApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.context = applicationContext
        startPush()
    }

    private fun startPush() {
        val options = firebaseOptions() ?: return
        val initialised = runCatching {
            FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this, options) != null
        }.getOrElse { e ->
            Log.w(TAG, "Firebase could not be initialised; push notifications are off", e)
            false
        }
        if (!initialised) return

        PushTokens.markAvailable()
        // Before any push can arrive: while the app is in the background,
        // Android draws pushes itself on the channel the manifest names, and
        // only uses it if it already exists.
        KickaboutMessagingService.createChannel(this)
        // Firebase caches the registration token, so this is usually instant.
        // It fails without Google Play services, which is not an error worth
        // showing anyone - the app simply gets no push on that device.
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                // task.result throws for a failed task, so check first.
                val token = if (task.isSuccessful) task.result else null
                if (token != null) {
                    PushTokens.update(PushPlatform.ANDROID, token)
                } else {
                    Log.w(TAG, "No push token yet", task.exception)
                }
            }
        }.onFailure { e -> Log.w(TAG, "Firebase messaging is unavailable", e) }
    }

    /** Null when this build wasn't given Firebase settings. */
    private fun firebaseOptions(): FirebaseOptions? {
        val projectId = BuildConfig.FIREBASE_PROJECT_ID
        val applicationId = BuildConfig.FIREBASE_APPLICATION_ID
        val apiKey = BuildConfig.FIREBASE_API_KEY
        val senderId = BuildConfig.FIREBASE_SENDER_ID
        if (projectId.isBlank() || applicationId.isBlank() || apiKey.isBlank() || senderId.isBlank()) return null
        return FirebaseOptions.Builder()
            .setProjectId(projectId)
            .setApplicationId(applicationId)
            .setApiKey(apiKey)
            .setGcmSenderId(senderId)
            .build()
    }

    private companion object {
        const val TAG = "KickaboutPush"
    }
}
