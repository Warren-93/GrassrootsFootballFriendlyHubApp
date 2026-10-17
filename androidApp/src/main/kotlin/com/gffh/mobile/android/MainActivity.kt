package com.gffh.mobile.android

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gffh.mobile.App
import com.gffh.mobile.core.auth.AndroidAppContext
import com.gffh.mobile.core.push.NotificationTap
import com.gffh.mobile.core.push.NotificationTaps

/**
 * Tapping a push lands here with the push's ids as intent extras - in
 * onCreate when it starts the app, in onNewIntent when the app is already
 * open (the manifest makes this singleTop, so the open screen isn't thrown
 * away for a new one). Either way they go to NotificationTaps, and App opens
 * the screen once someone is signed in.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.context = applicationContext
        // A recreated Activity (rotation, or reopened from Recents) gets the
        // intent it started with again - that tap has been dealt with already.
        val launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        if (savedInstanceState == null && !launchedFromHistory) handleNotificationTap(intent)
        setContent {
            App(apiBaseUrl = BuildConfig.API_BASE_URL)
        }
    }

    /**
     * The manifest keeps this Activity through rotation and dark mode switches
     * (configChanges), so the system bars' icon colours are picked again here
     * for the new theme - enableEdgeToEdge only reads it when called.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
    }

    /** Nothing happens for an ordinary launch, which has none of the keys. */
    private fun handleNotificationTap(intent: Intent?) {
        val extras = intent?.extras ?: return
        NotificationTaps.handle(NotificationTap.KEYS.associateWith { key -> extras.getString(key) })
    }
}
