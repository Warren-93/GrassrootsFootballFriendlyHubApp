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

/** Android plays GIFs (coil-gif); iOS shows their first frame, as Coil has no GIF decoder there. */
internal expect fun ComponentRegistry.Builder.addAnimatedGifDecoder()
