package com.gffh.mobile.core.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
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
        // A phone camera usually stores the photo sideways with an EXIF note
        // saying which way is up. BitmapFactory ignores that note (browsers
        // and iOS don't), so turn the picture upright before cropping it.
        val upright = rotated(decoded, exifRotationDegrees(resolver, uri))
        val side = minOf(upright.width, upright.height)
        val left = (upright.width - side) / 2
        val top = (upright.height - side) / 2

        val square = Bitmap.createBitmap(target, target, Bitmap.Config.ARGB_8888)
        Canvas(square).apply {
            // JPEG has no transparency: a see-through PNG gets a white
            // background, as it does on the web, rather than a black one.
            drawColor(Color.WHITE)
            drawBitmap(
                upright,
                Rect(left, top, left + side, top + side),
                Rect(0, 0, target, target),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
        }
        val bytes = ByteArrayOutputStream()
        square.compress(Bitmap.CompressFormat.JPEG, (BADGE_JPEG_QUALITY * 100).toInt(), bytes)
        square.recycle()
        if (upright !== decoded) upright.recycle()
        BadgePickResult.Picked(BADGE_DATA_URL_PREFIX + Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP))
    } catch (e: Exception) {
        BadgePickResult.Failed
    } finally {
        decoded.recycle()
    }
}

private fun sampleSizeFor(shortestSide: Int, target: Int): Int {
    var sample = 1
    while (target > 0 && shortestSide / (sample * 2) >= target) sample *= 2
    return sample
}

/** 0 for an image with no orientation note, or one that can't be read - it's then used as it is. */
private fun exifRotationDegrees(resolver: ContentResolver, uri: Uri): Int {
    val orientation = runCatching {
        resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
    }.getOrNull()
    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
}

private fun rotated(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
