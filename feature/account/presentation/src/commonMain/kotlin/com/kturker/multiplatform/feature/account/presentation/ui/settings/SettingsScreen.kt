package com.kturker.multiplatform.feature.account.presentation.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.account
import com.kturker.multiplatform.core.multilang.resource.sharedSpacesDeleteWarning
import com.kturker.multiplatform.feature.account.presentation.ui.SignInCallbacks
import com.kturker.multiplatform.feature.account.presentation.ui.SignInMethods
import com.kturker.multiplatform.feature.account.presentation.ui.uiErrorText
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions
) {
    val strings = LocalStringResources.current.account
    // Leaving would clear the ViewModel mid-deletion: the system back is swallowed until it has an answer
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.busy,
        onBackCompleted = {}
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.accountTitle) },
                navigationIcon = {
                    TextButton(onClick = actions::onBack, enabled = !state.busy) {
                        Text(strings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            // The keyboard does not resize the window (edge to edge, and iOS): shrink the column so the
            // buttons below the text fields can still be scrolled into view while typing
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.email?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
            OutlinedButton(onClick = actions::signOut, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                Text(strings.signOut)
            }
            OutlinedButton(
                onClick = actions::requestDelete,
                enabled = !state.busy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.deleteAccount)
            }
            if (state.needsReauth) ReauthSection(email = state.email.orEmpty(), busy = state.busy, actions = actions)
            state.error?.let { Text(uiErrorText(it), color = MaterialTheme.colorScheme.error) }
        }
    }

    if (state.confirmingDelete) {
        DeleteDialog(
            sharedSpaces = state.sharedSpacesToDelete,
            canConfirm = !state.loadingSharedSpaces,
            actions = actions
        )
    }
}

@Composable
private fun DeleteDialog(
    sharedSpaces: Int?,
    canConfirm: Boolean,
    actions: SettingsActions
) {
    val strings = LocalStringResources.current.account
    AlertDialog(
        onDismissRequest = actions::dismissDelete,
        title = { Text(strings.deleteAccount) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings.deleteAccountWarning)
                if (sharedSpaces != null && sharedSpaces > 0) {
                    Text(
                        strings.sharedSpacesDeleteWarning(sharedSpaces),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = actions::confirmDelete,
                enabled = canConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = { TextButton(onClick = actions::dismissDelete) { Text(strings.cancel) } }
    )
}

@Composable
private fun ReauthSection(
    email: String,
    busy: Boolean,
    actions: SettingsActions
) {
    val strings = LocalStringResources.current.account
    var typedEmail by rememberSaveable { mutableStateOf(email) }
    var password by rememberSaveable { mutableStateOf("") }
    Text(strings.signInAgainToDelete, style = MaterialTheme.typography.titleMedium)
    SignInMethods(
        email = typedEmail,
        password = password,
        enabled = !busy,
        callbacks = SignInCallbacks(
            onEmailChange = { typedEmail = it },
            onPasswordChange = { password = it },
            onSignIn = { actions.reauthWithEmail(typedEmail, password) },
            onGoogle = actions::reauthWithGoogle,
            onApple = actions::reauthWithApple
        )
    )
}
