package com.gffh.mobile.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.gffh.mobile.core.auth.AndroidAppContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * The Android photo picker (`PickVisualMedia`): it runs in the system's own
 * process, so it needs no storage permission and hands back one image URI.
 * Scaling and encoding run off the main thread - the picked file can be a
 * 12-megapixel photo.
 */
@Composable
actual fun rememberBadgeImagePicker(onResult: (BadgePickResult) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) {
            onResult(BadgePickResult.Cancelled)
        } else {
            scope.launch { onResult(withContext(Dispatchers.Default) { readBadge(uri) }) }
        }
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Square centre crop, at most [BADGE_MAX_SIZE_PX] a side, JPEG, as a data URL - same output as gffh-web's BadgePicker. */
private fun readBadge(uri: Uri): BadgePickResult {
    val resolver = AndroidAppContext.context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return BadgePickResult.Failed

    val shortestSide = minOf(bounds.outWidth, bounds.outHeight)
    val target = minOf(BADGE_MAX_SIZE_PX, shortestSide)
    // Decode at the smallest power-of-two scale that still covers the target square.
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSizeFor(shortestSide, target) }
    val decoded = runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } }
        .getOrNull() ?: return BadgePickResult.Failed

    return try {
        val side = minOf(decoded.width, decoded.height)
        val cropped = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
        val square = if (side == target) cropped else Bitmap.createScaledBitmap(cropped, target, target, true)
        val bytes = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.JPEG, (BADGE_JPEG_QUALITY * 100).toInt(), bytes)
        if (square !== cropped) cropped.recycle()
        if (cropped !== decoded) decoded.recycle()
        BadgePickResult.Picked(BADGE_DATA_URL_PREFIX + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
    } catch (e: Exception) {
        BadgePickResult.Failed
    }
}

private fun sampleSizeFor(shortestSide: Int, target: Int): Int {
    var sample = 1
    while (target > 0 && shortestSide / (sample * 2) >= target) sample *= 2
    return sample
}
