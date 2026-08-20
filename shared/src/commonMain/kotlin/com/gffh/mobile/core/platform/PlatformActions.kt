package com.gffh.mobile.core.platform

/** Opens the device's default mail client, if one exists. */
expect fun openMailApp()

/**
 * Hands [content] to the user as a real file named [filename] - the Android
 * Downloads folder directly, or the iOS share sheet (save to Files, AirDrop,
 * etc.) since iOS has no equivalent "just drop it in Downloads" API. Returns
 * false if it couldn't be offered (e.g. pre-Android-10, where the scoped
 * Downloads API this uses doesn't exist yet), so the caller can fall back to
 * showing the content on-screen instead.
 */
expect fun saveTextFile(filename: String, content: String): Boolean
