package com.gffh.mobile.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gffh.mobile.core.network.ApiResult
import com.gffh.mobile.model.AddMemberRequest
import com.gffh.mobile.model.MemberRole
import com.gffh.mobile.model.MemberView
import com.gffh.mobile.model.UpdateMemberRoleRequest
import com.gffh.mobile.navigation.Navigator
import com.gffh.mobile.repository.MemberRepository
import kotlinx.coroutines.launch

private fun roleLabel(role: MemberRole): String = when (role) {
    MemberRole.USER -> "No management role"
    MemberRole.TEAM_MANAGER -> "Team manager"
    MemberRole.CLUB_ADMIN -> "Club admin"
}

/**
 * SCR-PR-04 Team members and permissions. Purpose: control who can act on
 * behalf of a team - a safeguarding control as much as an admin one. Club
 * admins cascade to every squad the club runs, so promoting someone here
 * converts their membership to club-wide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(memberRepository: MemberRepository, navigator: Navigator, teamId: String) {
    var members by remember { mutableStateOf<List<MemberView>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var addRole by remember { mutableStateOf(MemberRole.TEAM_MANAGER) }
    var adding by remember { mutableStateOf(false) }
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

    LaunchedEffect(teamId) { load() }

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
            Text(
                "Club admins can manage every squad the club runs. Team managers can only manage this one.",
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
                                    IconButton(onClick = { removeTarget = m }, enabled = busyId != m.membershipId) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MemberRole.entries.forEach { r ->
                                    FilterChip(
                                        selected = currentRole == r,
                                        onClick = { changeRole(m, r) },
                                        enabled = busyId != m.membershipId,
                                        label = { Text(roleLabel(r)) }
                                    )
                                }
                            }
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
                MemberRole.entries.forEach { r ->
                    FilterChip(selected = addRole == r, onClick = { addRole = r }, label = { Text(roleLabel(r)) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    adding = true
                    errorMessage = null
                    scope.launch {
                        when (val result = memberRepository.add(teamId, AddMemberRequest(email.trim(), addRole.name))) {
                            is ApiResult.Success -> {
                                email = ""
                                addRole = MemberRole.TEAM_MANAGER
                                load()
                            }
                            is ApiResult.Failure -> errorMessage = result.message
                        }
                        adding = false
                    }
                },
                enabled = !adding && email.trim().length >= 3,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (adding) "Adding…" else "Add official") }
            Spacer(Modifier.height(4.dp))
            Text(
                "They'll need to already have an account with this email - there's no invitation email yet.",
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
