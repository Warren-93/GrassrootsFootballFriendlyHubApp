package com.gffh.mobile.core.platform

/**
 * Opens the device's default mail client. Returns false when there's none to
 * open, so the caller can say so.
 */
expect fun openMailApp(): Boolean

/**
 * Opens [url] in whichever app handles it - a Google Maps directions link in
 * Maps or the browser, any other https link in the browser. Returns false
 * when it plainly couldn't (no app installed for it), so the caller can say so.
 */
expect fun openUrl(url: String): Boolean

/**
 * Starts a new email to [address] in the device's mail app, with [subject]
 * filled in. Returns false when there's no mail app to hand it to.
 */
expect fun composeEmail(address: String, subject: String): Boolean

/**
 * Hands [content] to the user as a real file named [filename] - the Android
 * Downloads folder directly, or the iOS share sheet (save to Files, AirDrop,
 * etc.) since iOS has no equivalent "just drop it in Downloads" API. Returns
 * false if it couldn't be offered (e.g. pre-Android-10, where the scoped
 * Downloads API this uses doesn't exist yet), so the caller can fall back to
 * showing the content on-screen instead.
 */
expect fun saveTextFile(filename: String, content: String): Boolean
