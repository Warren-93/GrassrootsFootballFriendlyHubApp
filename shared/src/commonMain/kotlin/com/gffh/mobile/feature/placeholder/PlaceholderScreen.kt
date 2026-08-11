package com.gffh.mobile.feature.placeholder

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator

/**
 * Stands in for every screen beyond the Access slice while the Setup,
 * Publish, Discover and Arrange slices are built out - see
 * gffh-mobile/README.md for the build order this follows.
 */
@Composable
fun PlaceholderScreen(label: String, navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "This screen is not built yet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (navigator.canGoBack) {
            TextButton(onClick = { navigator.pop() }, modifier = Modifier.padding(top = 24.dp)) {
                Text("Back")
            }
        }
    }
}
