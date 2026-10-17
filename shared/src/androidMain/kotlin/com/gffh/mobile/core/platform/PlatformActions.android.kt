package com.gffh.mobile.core.platform

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.gffh.mobile.core.auth.AndroidAppContext

actual fun openMailApp(): Boolean {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_EMAIL)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    // startActivity throws ActivityNotFoundException when there's no mail app.
    return runCatching { AndroidAppContext.context.startActivity(intent) }.isSuccess
}

actual fun openUrl(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    // startActivity throws ActivityNotFoundException when nothing handles it.
    return runCatching { AndroidAppContext.context.startActivity(intent) }.isSuccess
}

/** ACTION_SENDTO with a mailto: URI offers only mail apps, not every app that can share text. */
actual fun composeEmail(address: String, subject: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address?subject=${Uri.encode(subject)}")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    return runCatching { AndroidAppContext.context.startActivity(intent) }.isSuccess
}

actual fun saveTextFile(filename: String, content: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    return runCatching {
        val resolver = AndroidAppContext.context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        true
    }.getOrDefault(false)
}
