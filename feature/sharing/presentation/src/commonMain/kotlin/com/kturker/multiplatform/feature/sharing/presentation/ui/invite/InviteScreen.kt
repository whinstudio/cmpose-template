package com.kturker.multiplatform.feature.sharing.presentation.ui.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.SharingStrings
import com.kturker.multiplatform.core.multilang.resource.inviteMessage
import com.kturker.multiplatform.core.multilang.resource.inviteRoleText
import com.kturker.multiplatform.core.multilang.resource.sharing
import com.kturker.multiplatform.feature.sharing.presentation.ui.label
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun InviteRoute(viewModel: InviteViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InviteScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InviteScreen(
    state: InviteUiState,
    actions: InviteActions
) {
    val strings = LocalStringResources.current.sharing
    Scaffold(topBar = { TopAppBar(title = { Text(strings.inviteTitle) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.loading || state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.preview?.takeIf { state.canAccept }?.let { preview ->
                Text(
                    strings.inviteMessage(inviterName = preview.inviterName, spaceName = preview.spaceName),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(strings.inviteRoleText(preview.role.label(strings)))
                Button(onClick = actions::accept, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.accept)
                }
                OutlinedButton(onClick = actions::close, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.decline)
                }
            }
            state.message?.let { message ->
                Text(message.text(strings), style = MaterialTheme.typography.bodyLarge)
                if (state.canRetry) {
                    OutlinedButton(onClick = actions::retry, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.retry)
                    }
                }
                OutlinedButton(onClick = actions::close, modifier = Modifier.fillMaxWidth()) { Text(strings.close) }
            }
        }
    }
}

private fun InviteMessage.text(strings: SharingStrings): String = when (this) {
    InviteMessage.USED -> strings.inviteUsed
    InviteMessage.EXPIRED -> strings.inviteExpired
    InviteMessage.REVOKED -> strings.inviteRevoked
    InviteMessage.SPACE_FULL -> strings.inviteSpaceFull
    InviteMessage.NOT_FOUND -> strings.inviteNotFound
    InviteMessage.RATE_LIMITED -> strings.errorRateLimited
    InviteMessage.NETWORK -> strings.errorNetwork
    InviteMessage.UNKNOWN -> strings.errorUnknown
}
