package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.core.validation.Validators
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.navigation.Route
import com.gffh.mobile.repository.AuthRepository
import com.gffh.mobile.session.CurrentTeamStore
import com.gffh.mobile.theme.eyebrowLabel
import com.gffh.mobile.ui.components.CrestAvatar
import com.gffh.mobile.ui.components.HeroBand
import kotlinx.coroutines.launch

/**
 * SCR-PR-08 Settings. Purpose: account, notification, privacy and support
 * controls in one predictable place. Change email and Change password open
 * dialogs that match gffh-web's Settings page.
 *
 * Units and Appearance aren't shown: neither has anywhere to persist to yet,
 * and a toggle that resets on relaunch would be worse than no toggle.
 */
@Composable
fun SettingsScreen(authRepository: AuthRepository, currentTeamStore: CurrentTeamStore, navigator: Navigator) {
    val session by authRepository.session.collectAsState()
    val activeTeam by currentTeamStore.active.collectAsState()
    var confirmSignOut by remember { mutableStateOf(false) }
    var changingEmail by remember { mutableStateOf(false) }
    var changingPassword by remember { mutableStateOf(false) }
    val signOutScope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        HeroBand(
            title = session?.displayName ?: "Settings",
            eyebrow = "Account",
            subtitle = activeTeam?.let { "Managing ${it.teamName}" } ?: "Manage your account and preferences.",
            compact = true,
            modifier = Modifier.padding(16.dp)
        )

        Column(Modifier.padding(horizontal = 24.dp)) {
        SectionLabel("Account")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(session?.displayName ?: "", style = MaterialTheme.typography.bodyMedium)
                Text(session?.email ?: "", style = MaterialTheme.typography.bodySmall)
                if (session?.emailVerified == false) {
                    Text("Unverified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    AssistChip(onClick = { navigator.push(Route.EmailVerification) }, label = { Text("Verify your email") })
                }
                Spacer(Modifier.height(4.dp))
                Row {
                    TextButton(onClick = { changingEmail = true }) { Text("Change email") }
                    TextButton(onClick = { changingPassword = true }) { Text("Change password") }
                }
            }
        }

        if (activeTeam != null) {
            val team = activeTeam!!
            Spacer(Modifier.height(20.dp))
            SectionLabel("Team & club")
            Card(
                onClick = { navigator.push(Route.TeamProfile(team.teamId)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CrestAvatar(team.teamName, size = 32.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(team.teamName, style = MaterialTheme.typography.titleSmall)
                        Text("Team profile", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            SettingsRow("Members") { navigator.push(Route.Members(team.teamId)) }
            SettingsRow("Club") { navigator.push(Route.ClubProfile(team.clubId)) }
            SettingsRow("Venues") { navigator.push(Route.VenuesList(team.clubId)) }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("General")
        SettingsRow("Notifications") { navigator.push(Route.NotificationPreferences) }
        SettingsRow("Privacy and data") { navigator.push(Route.Privacy) }
        SettingsRow("Blocked teams") { navigator.push(Route.BlockedTeams) }
        SettingsRow("Help and support") { navigator.push(Route.Help) }

        Spacer(Modifier.height(28.dp))
        SectionLabel("Danger zone")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "Signing out ends your session on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                TextButton(
                    onClick = { confirmSignOut = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Sign out") }
            }
        }
        Spacer(Modifier.height(24.dp))
        }
    }

    if (changingEmail) {
        ChangeEmailDialog(authRepository, currentEmail = session?.email ?: "", onDismiss = { changingEmail = false })
    }
    if (changingPassword) {
        ChangePasswordDialog(authRepository, onDismiss = { changingPassword = false })
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to publish availability or respond to requests.") },
            confirmButton = {
                TextButton(onClick = {
                    // Suspends only to withdraw this device's push token first.
                    signOutScope.launch {
                        authRepository.signOut()
                        currentTeamStore.clear()
                        navigator.resetTo(Route.Welcome)
                    }
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } }
        )
    }
}

/**
 * New email + current password -> `POST /api/v1/me/email`, as on gffh-web's
 * Settings page. The new address starts unverified: the API emails it a link,
 * or - while no email provider is connected - hands the token straight back,
 * so this offers "Verify now" instead, like EmailVerificationScreen.
 */
@Composable
private fun ChangeEmailDialog(authRepository: AuthRepository, currentEmail: String, onDismiss: () -> Unit) {
    var newEmail by remember { mutableStateOf(currentEmail) }
    var password by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var changedTo by remember { mutableStateOf<String?>(null) }
    var verifyToken by remember { mutableStateOf<String?>(null) }
    var verified by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val emailValid = Validators.isValidEmail(newEmail)

    fun save() {
        submitting = true
        errorMessage = null
        scope.launch {
            when (val result = authRepository.changeEmail(newEmail.trim(), password)) {
                is ApiResult.Success -> {
                    changedTo = result.value.user.email
                    verifyToken = result.value.verificationToken
                    password = ""
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            submitting = false
        }
    }

    fun verifyNow(token: String) {
        submitting = true
        errorMessage = null
        scope.launch {
            when (val result = authRepository.confirmVerification(token)) {
                is ApiResult.Success -> {
                    verifyToken = null
                    verified = true
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            submitting = false
        }
    }

    val done = changedTo
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change email") },
        text = {
            Column {
                if (done == null) {
                    Text("Your account becomes unverified again until you confirm the new address.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("New email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = newEmail.isNotBlank() && !emailValid,
                        supportingText = { if (newEmail.isNotBlank() && !emailValid) Text("Enter a valid email address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (verified) {
                    Text("Email changed to $done and verified.")
                } else {
                    val token = verifyToken
                    if (token == null) {
                        Text("Email changed to $done. It's unverified until you open the link we've emailed there.")
                    } else {
                        Text("Email changed to $done. It's unverified until you confirm it.")
                        Spacer(Modifier.height(12.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "No email provider is connected yet, so we can't send this automatically.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { verifyNow(token) },
                                    enabled = !submitting,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) { Text(if (submitting) "Verifying…" else "Verify now") }
                            }
                        }
                    }
                }
                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (done == null) {
                TextButton(
                    onClick = { save() },
                    enabled = !submitting && emailValid && password.isNotEmpty()
                ) { Text(if (submitting) "Saving…" else "Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (done == null) "Cancel" else "Close") } }
    )
}

/** Current + new password (10 characters or more, as at registration) -> `POST /api/v1/me/password`. */
@Composable
private fun ChangePasswordDialog(authRepository: AuthRepository, onDismiss: () -> Unit) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var changed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val newPasswordValid = Validators.isValidPassword(newPassword)

    fun save() {
        submitting = true
        errorMessage = null
        scope.launch {
            when (val result = authRepository.changePassword(currentPassword, newPassword)) {
                is ApiResult.Success -> {
                    changed = true
                    currentPassword = ""
                    newPassword = ""
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            submitting = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column {
                if (changed) {
                    Text("Password changed.")
                } else {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = newPassword.isNotEmpty() && !newPasswordValid,
                        supportingText = { Text("At least 10 characters") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            if (!changed) {
                TextButton(
                    onClick = { save() },
                    enabled = !submitting && currentPassword.isNotEmpty() && newPasswordValid
                ) { Text(if (submitting) "Saving…" else "Save") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (changed) "Close" else "Cancel") } }
    )
}

@Composable
private fun SettingsRow(label: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(label, modifier = Modifier.padding(16.dp))
    }
}

/** Small bold uppercase section header, matching the eyebrow treatment used above [HeroBand] titles. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = eyebrowLabel,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
