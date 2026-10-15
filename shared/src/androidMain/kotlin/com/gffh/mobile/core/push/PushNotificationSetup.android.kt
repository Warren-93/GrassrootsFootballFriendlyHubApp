package com.gffh.mobile.core.push

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gffh.mobile.core.auth.AndroidAppContext

@Composable
actual fun PushNotificationSetup(signedIn: Boolean) {
    val pushAvailable by PushTokens.available.collectAsState()
    var asked by remember { mutableStateOf(false) }
    // The answer needs no handling: a refusal just means no notifications are
    // shown, and Android itself won't ask twice.
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(signedIn, pushAvailable) {
        if (!signedIn || !pushAvailable || asked) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        val granted = AndroidAppContext.context
            .checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (granted) return@LaunchedEffect
        asked = true
        permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
