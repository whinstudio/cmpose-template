package com.kturker.multiplatform.feature.sharing.presentation.ui.members

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.PendingInvite
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.inviteDaysLeftText
import com.kturker.multiplatform.core.multilang.resource.sharing
import com.kturker.multiplatform.feature.sharing.presentation.ui.label
import com.kturker.multiplatform.feature.sharing.presentation.ui.retryable
import com.kturker.multiplatform.feature.sharing.presentation.ui.sharingErrorText
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@Composable
internal fun MembersRoute(viewModel: MembersViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MembersScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersScreen(
    state: MembersUiState,
    actions: MembersActions
) {
    val strings = LocalStringResources.current.sharing
    // The days left are counted from when the invites were last loaded
    val now = remember(state.invites) { Clock.System.now().toEpochMilliseconds() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.space?.name ?: strings.membersTitle) },
                navigationIcon = { TextButton(onClick = actions::onBack) { Text(strings.back) } },
                actions = {
                    if (state.canShare) {
                        TextButton(onClick = actions::startShare, enabled = !state.busy) { Text(strings.share) }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.error?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sharingErrorText(error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    if (error.retryable) {
                        TextButton(onClick = actions::retry) { Text(strings.retry) }
                    } else {
                        TextButton(onClick = actions::dismissError) { Text(strings.close) }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.members, key = Member::userId) { member ->
                    MemberCard(member = member, state = state, actions = actions)
                }
                if (state.canShare && state.invites.isNotEmpty()) {
                    item(key = PENDING_INVITES_KEY) {
                        Text(
                            strings.pendingInvites,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(state.invites, key = { "$PENDING_INVITES_KEY:${it.id}" }) { invite ->
                        PendingInviteCard(invite = invite, now = now, busy = state.busy, actions = actions)
                    }
                }
            }
        }
    }

    if (state.choosingInviteRole) InviteRoleDialog(actions = actions)
    state.removing?.let { RemoveDialog(member = it, actions = actions) }
    state.revoking?.let { RevokeDialog(invite = it, actions = actions) }
}

// The pending invites' heading, and the prefix of their rows' keys (apart from the members' user ids)
private const val PENDING_INVITES_KEY = "pendingInvites"

@Composable
private fun MemberCard(
    member: Member,
    state: MembersUiState,
    actions: MembersActions
) {
    val strings = LocalStringResources.current.sharing
    var choosingRole by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                val you = if (state.isYou(member)) " ${strings.you}" else ""
                Text(member.shownName(strings.unnamedMember) + you, style = MaterialTheme.typography.titleMedium)
                Text(member.role.label(strings), style = MaterialTheme.typography.bodySmall)
            }
            if (state.canManage(member)) {
                Column {
                    TextButton(onClick = { choosingRole = true }, enabled = !state.busy) { Text(strings.changeRole) }
                    DropdownMenu(expanded = choosingRole, onDismissRequest = { choosingRole = false }) {
                        MembersUiState.grantableRoles.forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label(strings)) },
                                onClick = {
                                    choosingRole = false
                                    if (role != member.role) actions.changeRole(member, role)
                                }
                            )
                        }
                    }
                }
                TextButton(
                    onClick = { actions.requestRemove(member) },
                    enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.remove)
                }
            }
        }
    }
}

// An invite link not yet used: the role it gives, the days it has left, and Revoke
@Composable
private fun PendingInviteCard(
    invite: PendingInvite,
    now: Long,
    busy: Boolean,
    actions: MembersActions
) {
    val strings = LocalStringResources.current.sharing
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(invite.role.label(strings), style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.inviteDaysLeftText(daysLeft(invite.expiresAt, now)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(
                onClick = { actions.requestRevoke(invite) },
                enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.revokeInvite)
            }
        }
    }
}

@Composable
private fun InviteRoleDialog(actions: MembersActions) {
    val strings = LocalStringResources.current.sharing
    AlertDialog(
        onDismissRequest = actions::dismissShare,
        title = { Text(strings.inviteAs) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MembersUiState.grantableRoles.forEach { role ->
                    OutlinedButton(onClick = { actions.share(role) }, modifier = Modifier.fillMaxWidth()) {
                        Text(role.label(strings))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = actions::dismissShare) { Text(strings.cancel) } }
    )
}

@Composable
private fun RemoveDialog(
    member: Member,
    actions: MembersActions
) {
    val strings = LocalStringResources.current.sharing
    AlertDialog(
        onDismissRequest = actions::dismissRemove,
        title = { Text(member.shownName(strings.unnamedMember)) },
        text = { Text(strings.removeMemberWarning) },
        confirmButton = {
            OutlinedButton(
                onClick = actions::confirmRemove,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.remove)
            }
        },
        dismissButton = { TextButton(onClick = actions::dismissRemove) { Text(strings.cancel) } }
    )
}

@Composable
private fun RevokeDialog(
    invite: PendingInvite,
    actions: MembersActions
) {
    val strings = LocalStringResources.current.sharing
    AlertDialog(
        onDismissRequest = actions::dismissRevoke,
        title = { Text(invite.role.label(strings)) },
        text = { Text(strings.revokeInviteWarning) },
        confirmButton = {
            OutlinedButton(
                onClick = actions::confirmRevoke,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.revokeInvite)
            }
        },
        dismissButton = { TextButton(onClick = actions::dismissRevoke) { Text(strings.cancel) } }
    )
}
