package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AddMemberOutcome
import com.gffh.mobile.model.AddMemberRequest
import com.gffh.mobile.model.InviteView
import com.gffh.mobile.model.MemberRole
import com.gffh.mobile.model.MemberView
import com.gffh.mobile.model.UpdateMemberRoleRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.MemberRepository
import com.gffh.mobile.session.CurrentTeamStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun roleLabel(role: MemberRole): String = when (role) {
    MemberRole.USER -> "No management role"
    MemberRole.TEAM_MANAGER -> "Team manager"
    MemberRole.CLUB_ADMIN -> "Club admin"
}

/**
 * The roles the user can give someone, adding or changing them. Only a club
 * admin can make someone a club admin (the API refuses a team manager with
 * CLUB_ADMIN_ONLY), so it's left out when [canManageClub] is false; null -
 * the API hasn't said - offers every role, as before.
 */
fun assignableRoles(canManageClub: Boolean?): List<MemberRole> =
    if (canManageClub == false) MemberRole.entries - MemberRole.CLUB_ADMIN else MemberRole.entries

/** Whether the user can change or remove a member with [role]: a club admin's membership is for club admins to change. */
fun canChangeMember(role: MemberRole, canManageClub: Boolean?): Boolean =
    role != MemberRole.CLUB_ADMIN || canManageClub != false

/**
 * [code] a character at a time ("K X 7 P 3 Q"), so a screen reader spells a
 * join code out instead of reading it as a word.
 */
fun spelledOut(code: String): String = code.toList().joinToString(" ")

/**
 * SCR-PR-04 Team members and permissions. Purpose: control who can act on
 * behalf of a team - a safeguarding control as much as an admin one. Club
 * admins cascade to every squad the club runs, so promoting someone here
 * converts their membership to club-wide.
 *
 * Adding an email with no account yet emails them an invitation instead; it
 * shows under Pending invitations (cancellable) until they sign up and verify
 * that address, when it becomes a real membership.
 *
 * A team manager isn't offered the club admin role, nor a club admin's role
 * or remove controls - see [assignableRoles] and [canChangeMember].
 *
 * The team's join code sits at the top, for a manager to hand out - see
 * [JoinCodeCard].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(memberRepository: MemberRepository, currentTeamStore: CurrentTeamStore, navigator: Navigator, teamId: String) {
    val activeTeam by currentTeamStore.active.collectAsState()
    // Whether the user is an admin of this team's club, as far as the active team says.
    val canManageClub = activeTeam?.takeIf { it.teamId == teamId }?.canManageClub
    val roles = assignableRoles(canManageClub)
    var members by remember { mutableStateOf<List<MemberView>>(emptyList()) }
    var invites by remember { mutableStateOf<List<InviteView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var addNotice by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var addRole by remember { mutableStateOf(MemberRole.TEAM_MANAGER) }
    var adding by remember { mutableStateOf(false) }
    // A membership or invitation id while its change is in flight.
    var busyId by remember { mutableStateOf<String?>(null) }
    var removeTarget by remember { mutableStateOf<MemberView?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            when (val result = memberRepository.list(teamId)) {
                is ApiResult.Success -> members = result.value
                is ApiResult.Failure -> errorMessage = result.message
            }
            loading = false
        }
    }

    fun loadInvites() {
        scope.launch {
            // Pending invitations are secondary - if they won't load, the members list still stands.
            invites = when (val result = memberRepository.listInvites(teamId)) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> emptyList()
            }
        }
    }

    LaunchedEffect(teamId) {
        load()
        loadInvites()
    }

    fun add() {
        adding = true
        errorMessage = null
        addNotice = null
        scope.launch {
            when (val result = memberRepository.add(teamId, AddMemberRequest(email.trim(), addRole.name))) {
                is ApiResult.Success -> {
                    email = ""
                    addRole = MemberRole.TEAM_MANAGER
                    when (val outcome = result.value) {
                        is AddMemberOutcome.Added -> {
                            addNotice = "${outcome.member.displayName} has been added to the team"
                            load()
                        }
                        // No account with that email yet: the API emailed them an invitation instead.
                        is AddMemberOutcome.Invited -> {
                            addNotice = "Invitation sent to ${outcome.invite.email}"
                            loadInvites()
                        }
                    }
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            adding = false
        }
    }

    fun cancelInvite(invite: InviteView) {
        busyId = invite.id
        errorMessage = null
        scope.launch {
            when (val result = memberRepository.cancelInvite(teamId, invite.id)) {
                is ApiResult.Success -> invites = invites.filter { it.id != invite.id }
                is ApiResult.Failure -> errorMessage = result.message
            }
            busyId = null
        }
    }

    fun changeRole(member: MemberView, role: MemberRole) {
        if (role.name == member.role) return
        busyId = member.membershipId
        errorMessage = null
        scope.launch {
            when (val result = memberRepository.updateRole(teamId, member.membershipId, UpdateMemberRoleRequest(role.name))) {
                is ApiResult.Success -> load()
                is ApiResult.Failure -> errorMessage = result.message
            }
            busyId = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Team members and permissions") },
            navigationIcon = {
                IconButton(onClick = { navigator.pop() }) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            }
        )

        Column(Modifier.padding(16.dp).weight(1f).verticalScroll(rememberScrollState())) {
            JoinCodeCard(memberRepository, teamId)
            Spacer(Modifier.height(16.dp))

            Text(
                "Club admins can manage every squad the club runs. Team managers can only manage this one." +
                    (if (canManageClub == false) " Only a club admin can add, change or remove club admins." else ""),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (members.isEmpty()) {
                Text("No officials yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                members.forEach { m ->
                    val currentRole = MemberRole.entries.find { it.name == m.role } ?: MemberRole.USER
                    val canChange = canChangeMember(currentRole, canManageClub)
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(m.displayName, style = MaterialTheme.typography.titleSmall)
                                    Text(m.email, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (m.scope == "CLUB") {
                                        AssistChip(onClick = {}, label = { Text("Club-wide") })
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    if (canChange) {
                                        IconButton(onClick = { removeTarget = m }, enabled = busyId != m.membershipId) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                        }
                                    }
                                }
                            }
                            if (canChange) {
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    roles.forEach { r ->
                                        FilterChip(
                                            selected = currentRole == r,
                                            onClick = { changeRole(m, r) },
                                            enabled = busyId != m.membershipId,
                                            label = { Text(roleLabel(r)) }
                                        )
                                    }
                                }
                            } else {
                                Spacer(Modifier.height(4.dp))
                                Text(roleLabel(currentRole), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (invites.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Pending invitations", style = MaterialTheme.typography.titleSmall)
                Text(
                    "They'll join the team once they've signed up and verified their email.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                invites.forEach { invite ->
                    val inviteRole = MemberRole.entries.find { it.name == invite.role }
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.MailOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(invite.email, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    listOfNotNull(
                                        inviteRole?.let { roleLabel(it) },
                                        invite.expiresAt?.let { "expires ${it.take(10)}" }
                                    ).joinToString(" - "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(
                                onClick = { cancelInvite(invite) },
                                enabled = busyId != invite.id,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text(if (busyId == invite.id) "Cancelling…" else "Cancel") }
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Text("Add an official", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                roles.forEach { r ->
                    FilterChip(selected = addRole == r, onClick = { addRole = r }, label = { Text(roleLabel(r)) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { add() },
                enabled = !adding && email.trim().length >= 3,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (adding) "Adding…" else "Add official") }
            Spacer(Modifier.height(4.dp))
            addNotice?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                "If they don't have an account yet, we'll email them an invitation to join.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    removeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove ${target.displayName}?") },
            text = {
                Text(
                    "They'll lose management access " +
                        (if (target.scope == "CLUB") "to every squad in this club." else "to this squad.") +
                        " They'd need to be re-added to get it back."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    removeTarget = null
                    busyId = target.membershipId
                    scope.launch {
                        when (val result = memberRepository.remove(teamId, target.membershipId)) {
                            is ApiResult.Success -> load()
                            is ApiResult.Failure -> errorMessage = result.message
                        }
                        busyId = null
                    }
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } }
        )
    }
}

/**
 * The team's join code, for a manager to hand out. Players and parents enter
 * it in Kickabout to join the team as a basic member (role USER), with
 * read-only access, without anyone adding them by email. Only someone who can
 * manage the team can read or regenerate it (the API answers 404 otherwise).
 * There's one code per team: regenerating replaces it, and the old one stops
 * working straight away, so it asks first.
 */
@Composable
private fun JoinCodeCard(memberRepository: MemberRepository, teamId: String) {
    // Deprecated for LocalClipboard, but that one's ClipEntry is built
    // differently on Android and iOS - this takes plain text in common code.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var code by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var regenerating by remember { mutableStateOf(false) }
    // The code the user has asked to replace, while they confirm.
    var regenerateTarget by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true
        errorMessage = null
        scope.launch {
            when (val result = memberRepository.joinCode(teamId)) {
                is ApiResult.Success -> code = result.value.code
                is ApiResult.Failure -> errorMessage = result.message
            }
            loading = false
        }
    }

    fun regenerate() {
        regenerating = true
        errorMessage = null
        scope.launch {
            when (val result = memberRepository.regenerateJoinCode(teamId)) {
                is ApiResult.Success -> {
                    code = result.value.code
                    copied = false
                }
                is ApiResult.Failure -> errorMessage = result.message
            }
            regenerating = false
        }
    }

    LaunchedEffect(teamId) { load() }

    // "Copied" stands in for Copy briefly, then it's back to Copy.
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Join code", style = MaterialTheme.typography.titleSmall)
            Text(
                "Share it with players and parents - they use it to join the team in Kickabout, with read-only access.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))

            val current = code
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (current != null) {
                Text(
                    current,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 6.sp),
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "Join code ${spelledOut(current)}" }
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(current))
                            copied = true
                        },
                        enabled = !regenerating
                    ) { Text(if (copied) "Copied" else "Copy") }
                    TextButton(onClick = { regenerateTarget = current }, enabled = !regenerating) {
                        Text(if (regenerating) "Regenerating…" else "Regenerate")
                    }
                }
            }

            errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                // Nothing to show without a code, so offer another go at loading it.
                if (current == null) {
                    TextButton(onClick = { load() }) { Text("Try again") }
                }
            }
        }
    }

    regenerateTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { regenerateTarget = null },
            title = { Text("Regenerate the join code?") },
            text = {
                Text(
                    "The current code, $target, will stop working straight away - anyone who hasn't joined " +
                        "with it yet will need the new one. Everyone who's already joined stays on the team."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    regenerateTarget = null
                    regenerate()
                }) { Text("Regenerate") }
            },
            dismissButton = { TextButton(onClick = { regenerateTarget = null }) { Text("Cancel") } }
        )
    }
}
