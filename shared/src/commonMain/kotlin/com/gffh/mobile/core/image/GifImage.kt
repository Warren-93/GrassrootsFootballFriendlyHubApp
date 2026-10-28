package com.gffh.mobile.core.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * A chat GIF from [url], playing. Android hands it to Coil, whose GIF decoder
 * animates it; Coil can't animate on iOS, so there the frames are decoded and
 * played with Skia (the library Compose draws with) instead. [contentScale]
 * is Fit or Crop - the two the chat uses.
 */
@Composable
expect fun GifImage(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier = Modifier)
