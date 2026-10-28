package com.gffh.mobile.core.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Downloads the GIF (GIPHY's small renditions, cached in memory for the
 * session), then plays it: one Skia bitmap per GIF on screen, which each
 * frame is decoded into in turn, and a Canvas that redraws when it changes.
 * Off screen (scrolled out of the chat or the picker) it stops, as its
 * effects leave composition with it. Until it has loaded it takes up its
 * space empty, as Coil's AsyncImage does.
 */
@Composable
actual fun GifImage(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier) {
    val described = if (contentDescription == null) modifier else modifier.semantics { this.contentDescription = contentDescription }
    var bytes by remember(url) { mutableStateOf(GifCache.get(url)) }
    LaunchedEffect(url) { if (bytes == null) bytes = GifCache.load(url) }

    val gif = bytes
    if (gif == null) {
        Box(described)
        return
    }
    val player = remember(gif) { GifPlayer.open(gif) }
    if (player == null) {
        Box(described)
        return
    }
    DisposableEffect(player) { onDispose { player.close() } }

    // Read while drawing, so each new frame redraws the Canvas without recomposing.
    var frameShown by remember(player) { mutableIntStateOf(0) }
    LaunchedEffect(player) {
        if (player.frameCount <= 1) return@LaunchedEffect
        var frame = 0
        while (true) {
            delay(player.durationMillis(frame))
            frame = (frame + 1) % player.frameCount
            player.decode(frame)
            frameShown++
        }
    }

    val image = remember(player) { player.bitmap.asComposeImageBitmap() }
    Canvas(described) {
        if (frameShown < 0) return@Canvas
        val sourceWidth = image.width.toFloat()
        val sourceHeight = image.height.toFloat()
        val scale = if (contentScale == ContentScale.Crop) {
            max(size.width / sourceWidth, size.height / sourceHeight)
        } else {
            min(size.width / sourceWidth, size.height / sourceHeight)
        }
        val width = (sourceWidth * scale).roundToInt()
        val height = (sourceHeight * scale).roundToInt()
        clipRect {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
                dstSize = IntSize(width, height)
            )
        }
    }
}

/** One GIF's decoder and the bitmap its current frame is in. Main thread only. */
private class GifPlayer private constructor(private val codec: Codec, val bitmap: Bitmap) {

    val frameCount: Int = codec.frameCount

    private val durations: List<Int> = codec.framesInfo.map { it.duration }

    /** Browsers play a frame of 10ms or less as 100ms, and GIFs are made to look right there. */
    fun durationMillis(frame: Int): Long = durations.getOrElse(frame) { 0 }.let { if (it <= 10) 100L else it.toLong() }

    fun decode(frame: Int) {
        codec.readPixels(bitmap, frame)
        bitmap.notifyPixelsChanged()
    }

    fun close() {
        bitmap.close()
        codec.close()
    }

    companion object {
        fun open(bytes: ByteArray): GifPlayer? = try {
            val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
            val bitmap = Bitmap()
            if (!bitmap.allocPixels(codec.imageInfo)) {
                codec.close()
                null
            } else {
                GifPlayer(codec, bitmap).also { it.decode(0) }
            }
        } catch (e: Exception) {
            null
        }
    }
}

/** The downloaded GIFs, newest last, so scrolling back up a chat doesn't fetch them again. */
private object GifCache {
    private const val MAX_ENTRIES = 60
    private val client by lazy { HttpClient(Darwin) }
    private val entries = LinkedHashMap<String, ByteArray>()

    fun get(url: String): ByteArray? = entries[url]

    suspend fun load(url: String): ByteArray? {
        entries[url]?.let { return it }
        val bytes = try {
            val response = client.get(url)
            if (response.status.isSuccess()) response.readBytes() else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return null
        entries[url] = bytes
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
        return bytes
    }
}
