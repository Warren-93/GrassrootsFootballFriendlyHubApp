package com.gffh.mobile.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gffh.mobile.App
import com.gffh.mobile.core.auth.AndroidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.context = applicationContext
        setContent {
            App(apiBaseUrl = BuildConfig.API_BASE_URL)
        }
    }
}
