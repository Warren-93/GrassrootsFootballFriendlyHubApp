package com.gffh.mobile.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.GifView
import com.gffh.mobile.repository.GifRepository
import kotlinx.coroutines.delay

private const val SEARCH_DELAY_MS = 400L

/**
 * The GIF picker: trending GIFs, or a search, from GIPHY through the API
 * (which keeps the key and the safe rating). Picking one sends it straight
 * away. Mirrors gffh-web's GifPickerButton.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifPickerSheet(
    gifRepository: GifRepository,
    onPick: (GifView) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var gifs by remember { mutableStateOf<List<GifView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        // Wait for typing to pause, so a search isn't sent per keystroke.
        if (query.isNotBlank()) delay(SEARCH_DELAY_MS)
        loading = true
        error = null
        val result = if (query.isBlank()) gifRepository.trending() else gifRepository.search(query.trim())
        when (result) {
            is ApiResult.Success -> gifs = result.value.results
            is ApiResult.Failure -> error = result.message
        }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search GIFs") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(360.dp)) {
                when {
                    error != null -> Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    loading && gifs.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    gifs.isEmpty() -> Text("No GIFs found.", modifier = Modifier.align(Alignment.Center))
                    else -> LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(gifs, key = { it.id }) { gif ->
                            AsyncImage(
                                model = gif.previewUrl,
                                contentDescription = gif.title.ifBlank { "GIF" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(gifAspectRatio(gif))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onPick(gif) }
                            )
                        }
                    }
                }
            }
            // GIPHY's terms ask for this attribution wherever their search is used.
            Text(
                "Powered by GIPHY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End).padding(vertical = 8.dp)
            )
        }
    }
}

/** Width over height, from GIPHY's sizes; square when they're missing. */
internal fun gifAspectRatio(gif: GifView): Float =
    if (gif.width > 0 && gif.height > 0) gif.width.toFloat() / gif.height else 1f
