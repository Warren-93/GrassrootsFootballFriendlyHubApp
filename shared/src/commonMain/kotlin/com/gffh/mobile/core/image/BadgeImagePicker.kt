package com.gffh.mobile.core.image

import androidx.compose.runtime.Composable

/** What came back from the platform's photo picker. */
sealed class BadgePickResult {
    /** [dataUrl] is ready to send as `badgeUrl` - see [BADGE_DATA_URL_PREFIX]. */
    data class Picked(val dataUrl: String) : BadgePickResult()

    /** The user backed out of the picker. */
    object Cancelled : BadgePickResult()

    /** Something was picked but couldn't be read as an image. */
    object Failed : BadgePickResult()
}

/**
 * Lets the user pick one image from their photos and hands it back as a
 * square JPEG data URL, at most [BADGE_MAX_SIZE_PX] a side - Android's photo
 * picker, iOS's PHPickerViewController. Call the returned function from a
 * button; [onResult] arrives later, on the main thread.
 *
 * Neither picker needs a storage permission: both run out of process and
 * return only what the user chose.
 */
@Composable
expect fun rememberBadgeImagePicker(onResult: (BadgePickResult) -> Unit): () -> Unit
