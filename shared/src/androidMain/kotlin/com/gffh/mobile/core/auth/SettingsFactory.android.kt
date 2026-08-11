package com.gffh.mobile.core.auth

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

/** Set once from [android.app.Application.onCreate] or the launcher Activity, before any Settings use. */
object AndroidAppContext {
    lateinit var context: Context
}

actual fun createSettings(): Settings {
    val prefs = AndroidAppContext.context.getSharedPreferences("gffh_session", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(prefs)
}
