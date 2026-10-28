package com.gffh.mobile.core.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

@Composable
actual fun GifImage(url: String, contentDescription: String?, contentScale: ContentScale, modifier: Modifier) {
    AsyncImage(model = url, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
}
