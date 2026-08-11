package com.gffh.mobile.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openMailApp() {
    val url = NSURL(string = "message://")
    if (UIApplication.sharedApplication.canOpenURL(url)) {
        UIApplication.sharedApplication.openURL(url)
    }
}
