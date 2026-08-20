package com.gffh.mobile.core.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

actual fun openMailApp() {
    val url = NSURL(string = "message://")
    if (UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    }
}

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
