package com.gffh.mobile.core.image

import coil3.ComponentRegistry

internal actual fun ComponentRegistry.Builder.addAnimatedGifDecoder() {
    // Coil has no animated GIF decoder on iOS yet; the first frame is shown.
}
