package com.gffh.mobile.core.platform

import android.content.Intent
import com.gffh.mobile.core.auth.AndroidAppContext

actual fun openMailApp() {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_EMAIL)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { AndroidAppContext.context.startActivity(intent) }
}
