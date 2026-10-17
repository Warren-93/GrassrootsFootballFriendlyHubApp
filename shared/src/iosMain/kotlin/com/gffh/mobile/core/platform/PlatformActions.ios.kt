package com.gffh.mobile.core.platform

import io.ktor.http.encodeURLParameter
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

actual fun openMailApp(): Boolean = openUrl("message://")

/**
 * Uses openURL(_:options:completionHandler:) - the one-argument openURL(_:)
 * is deprecated and, from iOS 18, refuses to open anything. canOpenURL isn't
 * checked first: it only answers for schemes listed under
 * LSApplicationQueriesSchemes, and opening an unhandled URL is a harmless no-op.
 */
actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
    return true
}

actual fun composeEmail(address: String, subject: String): Boolean =
    openUrl("mailto:$address?subject=${subject.encodeURLParameter()}")

/** No "save to Downloads" equivalent on iOS - the share sheet (Save to Files, AirDrop, etc.) is the closest fit. */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
actual fun saveTextFile(filename: String, content: String): Boolean {
    val cachesDir = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
        .firstOrNull()?.toString() ?: return false
    val path = "$cachesDir/$filename"

    val file = fopen(path, "w") ?: return false
    fputs(content, file)
    fclose(file)

    val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    val activityController = UIActivityViewController(
        activityItems = listOf(NSURL.fileURLWithPath(path)), applicationActivities = null
    )
    rootViewController.presentViewController(activityController, animated = true, completion = null)
    return true
}
