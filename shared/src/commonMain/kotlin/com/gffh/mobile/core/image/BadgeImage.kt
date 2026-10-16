package com.gffh.mobile.core.image

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64

/**
 * Team and club badges, as `team.badgeUrl` carries them.
 *
 * The API stores a badge either as an https link or as a small uploaded image
 * inlined as a data URL (`TeamDtos.BADGE_PATTERN` on the backend). gffh-web's
 * BadgePicker.tsx produces the uploaded form: a square centre crop, at most
 * 256px a side, JPEG at quality 0.85, as a `data:image/jpeg;base64,...` URL.
 * The platform pickers here produce exactly that, so a badge uploaded from
 * the phone looks the same everywhere.
 *
 * An https badge isn't fetched: this client has no image-loading library, and
 * nothing in the app writes one. Those fall back to the team's initials.
 */
const val BADGE_MAX_SIZE_PX: Int = 256

/** Matches the web app's canvas.toDataURL('image/jpeg', 0.85). */
const val BADGE_JPEG_QUALITY: Double = 0.85

const val BADGE_DATA_URL_PREFIX: String = "data:image/jpeg;base64,"

/**
 * Decodes an inlined badge for display. Returns null for a blank value, an
 * https link, an unsupported image type or anything that won't decode - the
 * caller then shows initials instead, exactly as before badges existed.
 */
@OptIn(
    kotlin.io.encoding.ExperimentalEncodingApi::class,
    org.jetbrains.compose.resources.ExperimentalResourceApi::class
)
fun decodeBadge(badgeUrl: String?): ImageBitmap? {
    val value = badgeUrl?.trim()
    if (value.isNullOrEmpty() || !value.startsWith("data:image/")) return null
    val base64 = value.substringAfter("base64,", missingDelimiterValue = "")
    if (base64.isEmpty()) return null
    return runCatching { Base64.decode(base64).decodeToImageBitmap() }.getOrNull()
}
