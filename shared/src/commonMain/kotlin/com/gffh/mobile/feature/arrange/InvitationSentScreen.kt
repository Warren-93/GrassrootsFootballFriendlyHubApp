package com.gffh.mobile.feature.arrange

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route

/** SCR-IN-03 Invitation sent. Purpose: confirm the send and set an expectation. */
@Composable
fun InvitationSentScreen(navigator: Navigator, requestId: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Invitation sent", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Most managers respond within a couple of days.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { navigator.resetTo(Route.RequestDetail(requestId)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("View request") }
        OutlinedButton(
            onClick = { navigator.resetTo(Route.SearchEntry) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) { Text("Find another") }
        TextButton(
            onClick = { navigator.resetTo(Route.Home) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Done") }
    }
}
