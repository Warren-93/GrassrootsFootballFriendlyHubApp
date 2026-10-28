package com.gffh.mobile.core.image

import androidx.compose.runtime.Composable
import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory

/**
 * Sets up Coil, which shows the chat's GIFs (from GIPHY's media servers).
 * Network loading comes from coil-network-ktor2 on its own; animated GIF
 * decoding is per platform - see [addAnimatedGifDecoder].
 */
@Composable
fun GifImageLoaderSetup() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { addAnimatedGifDecoder() }
            .build()
    }
}

/** Android plays GIFs with coil-gif; Coil has no GIF decoder on iOS, where [GifImage] plays them itself. */
internal expect fun ComponentRegistry.Builder.addAnimatedGifDecoder()
