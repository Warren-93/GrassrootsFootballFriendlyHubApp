package com.gffh.mobile.core.image

import android.os.Build
import coil3.ComponentRegistry
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder

internal actual fun ComponentRegistry.Builder.addAnimatedGifDecoder() {
    // ImageDecoder (Android 9+) is faster and uses less memory than the older GIF decoder.
    if (Build.VERSION.SDK_INT >= 28) add(AnimatedImageDecoder.Factory()) else add(GifDecoder.Factory())
}
