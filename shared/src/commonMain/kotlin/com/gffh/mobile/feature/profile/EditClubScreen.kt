package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.UpdateClubRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.ClubRepository
import kotlinx.coroutines.launch

/** SCR-PR-03's "Edit club" action - inline edit of the SCR-ON-02 fields. */
@Composable
fun EditClubScreen(clubRepository: ClubRepository, navigator: Navigator, clubId: String) {
    var loading by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var badgeUrl by remember { mutableStateOf("") }
    var postcode by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clubId) {
        when (val result = clubRepository.get(clubId)) {
            is ApiResult.Success -> {
                name = result.value.name
                badgeUrl = result.value.badgeUrl ?: ""
                postcode = result.value.postcode ?: ""
                website = result.value.website ?: ""
                contactEmail = result.value.contactEmail ?: ""
            }
            is ApiResult.Failure -> errorMessage = result.message
        }
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Edit club", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Club name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = badgeUrl, onValueChange = { badgeUrl = it }, label = { Text("Badge URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = postcode, onValueChange = { postcode = it }, label = { Text("Postcode") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = contactEmail, onValueChange = { contactEmail = it }, label = { Text("Contact email") }, modifier = Modifier.fillMaxWidth())

        errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                submitting = true
                errorMessage = null
                scope.launch {
                    val result = clubRepository.update(
                        clubId,
                        UpdateClubRequest(
                            name = name, badgeUrl = badgeUrl.ifBlank { null }, postcode = postcode,
                            website = website.ifBlank { null }, contactEmail = contactEmail.ifBlank { null }
                        )
                    )
                    submitting = false
                    when (result) {
                        is ApiResult.Success -> navigator.pop()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            },
            enabled = !submitting && name.isNotBlank() && postcode.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (submitting) "Saving…" else "Save club") }
        TextButton(onClick = { navigator.pop() }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
