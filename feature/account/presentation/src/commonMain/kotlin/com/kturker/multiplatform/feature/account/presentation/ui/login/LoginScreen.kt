package com.kturker.multiplatform.feature.account.presentation.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.account
import com.kturker.multiplatform.feature.account.presentation.ui.LegalLinksRow
import com.kturker.multiplatform.feature.account.presentation.ui.SignInCallbacks
import com.kturker.multiplatform.feature.account.presentation.ui.SignInMethods
import com.kturker.multiplatform.feature.account.presentation.ui.uiErrorText
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LoginRoute(viewModel: LoginViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(
    state: LoginUiState,
    actions: LoginActions
) {
    val strings = LocalStringResources.current.account
    val title = if (state.creatingAccount) strings.createAccount else strings.signIn
    // Back on the sign-up form returns to the sign-in form instead of leaving the app
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.creatingAccount,
        onBackCompleted = actions::backToSignIn
    )
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        // The keyboard does not resize the window (edge to edge, and iOS): shrink the column so the buttons
        // below the text fields can still be scrolled into view while typing
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            LoginContent(state = state, actions = actions, modifier = Modifier.weight(1f))
            // Outside the scroll: the way back stays above the keyboard while the scroll keeps the focused
            // field in view (the three sign-up fields and both buttons do not fit above it on every phone)
            if (state.creatingAccount) {
                TextButton(
                    onClick = actions::backToSignIn,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                ) {
                    Text(strings.alreadyHaveAccount)
                }
            }
        }
    }
}

@Composable
private fun LoginContent(
    state: LoginUiState,
    actions: LoginActions,
    modifier: Modifier
) {
    val strings = LocalStringResources.current.account
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        if (state.firebaseUnavailable) {
            Text(strings.firebaseUnavailable, color = MaterialTheme.colorScheme.error)
        }
        if (state.sessionEnded) {
            Text(strings.sessionEnded, color = MaterialTheme.colorScheme.error)
        }
        if (state.creatingAccount) {
            CreateAccountForm(state = state, actions = actions)
        } else {
            SignInMethods(
                email = state.email,
                password = state.password,
                enabled = state.signInEnabled,
                callbacks = SignInCallbacks(
                    onEmailChange = actions::onEmailChange,
                    onPasswordChange = actions::onPasswordChange,
                    onSignIn = actions::signIn,
                    onGoogle = actions::signInWithGoogle,
                    onApple = actions::signInWithApple,
                    onCreateAccount = actions::startCreatingAccount
                )
            )
        }
        state.error?.let { Text(uiErrorText(it), color = MaterialTheme.colorScheme.error) }
        // Below both forms: the privacy policy is reachable before an account is made
        LegalLinksRow(links = state.legalLinks, onOpen = uriHandler::openUri)
    }
}
