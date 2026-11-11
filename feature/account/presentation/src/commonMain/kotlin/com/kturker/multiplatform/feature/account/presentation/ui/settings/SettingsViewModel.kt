package com.kturker.multiplatform.feature.account.presentation.ui.settings

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.auth.signOutAndForget
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.account.presentation.data.AccountRepository
import com.kturker.multiplatform.feature.account.presentation.data.PendingDisplayName
import com.kturker.multiplatform.feature.account.presentation.data.sharedOwnedCount
import com.kturker.multiplatform.feature.account.presentation.ui.UiError
import com.kturker.multiplatform.feature.account.presentation.ui.isUnauthorized
import com.kturker.multiplatform.feature.account.presentation.ui.isUpgradeRequired
import com.kturker.multiplatform.feature.account.presentation.ui.toUiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/**
 * Sign-out and account deletion. The confirmation names the shared spaces the deletion takes from their members
 * (GET /spaces). Deletion checks with Firebase first (canDeleteNow), then deletes the server's data (DELETE /me),
 * then the Firebase user: an old session asks for a fresh sign-in before anything is deleted. Should Firebase still
 * refuse at the end, the same sign-in starts the whole flow again; the server answers the repeated DELETE /me
 * with 204. An Apple user signs in again before the deletion, because Firebase does not keep the Apple token and
 * revoking it does not block the deletion. Once the Firebase user is gone, so is a sign-up name of theirs still
 * waiting for Firebase ([PendingDisplayName]): personal data does not outlive the account.
 */
@KoinViewModel
internal class SettingsViewModel(
    private val repository: AccountRepository,
    @Provided private val auth: AuthService,
    @Provided private val tokens: SessionTokens,
    @Provided private val navigationManager: NavigationManager,
    private val pendingName: PendingDisplayName,
    @Provided appInfo: AppInfo
) : CoreViewModel(),
    SettingsActions {

    private val _state = MutableStateFlow(SettingsUiState(legalLinks = appInfo.legalLinks))
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    // The account the deletion was confirmed for; a sign-in with another one stops it
    private var deletingUid: String? = null
    private var sharedSpacesLoad: Job? = null

    init {
        viewModelScope.launch {
            auth.state.collect { auth ->
                _state.update { it.copy(email = (auth as? AuthState.SignedIn)?.email) }
            }
        }
    }

    // Leaving clears the ViewModel: a deletion in progress stays on this screen until it has an answer
    override fun onBack() {
        if (_state.value.busy) return
        navigationManager.navigate(NavigationCommand.NavigateUp)
    }

    override fun signOut() {
        viewModelScope.launch { signOutAndForget(SignOutReason.USER) }
    }

    // The backend client caches the bearer token: dropped with the session, so the next user's calls never
    // carry this one's
    private suspend fun signOutAndForget(reason: SignOutReason) =
        auth.signOutAndForget(tokens, reason)

    override fun requestDelete() {
        _state.update {
            it.copy(confirmingDelete = true, sharedSpacesToDelete = null, loadingSharedSpaces = true, error = null)
        }
        sharedSpacesLoad?.cancel()
        // The dialog opens at once; the count follows, and Delete waits for it. Without an answer the dialog shows
        // the general warning only.
        sharedSpacesLoad = viewModelScope.launch {
            val spaces = repository.spaces()
            val count = (spaces as? RestResult.Success)?.result?.sharedOwnedCount()
            _state.update { it.copy(sharedSpacesToDelete = count, loadingSharedSpaces = false) }
        }
    }

    override fun dismissDelete() = _state.update { it.copy(confirmingDelete = false) }

    override fun confirmDelete() {
        if (_state.value.busy || _state.value.loadingSharedSpaces) return
        deletingUid = auth.currentUid()
        _state.update { it.copy(confirmingDelete = false, busy = true, error = null) }
        viewModelScope.launch { if (auth.usesAppleSignIn()) signInWithAppleThenDelete() else deleteAccount() }
    }

    private suspend fun signInWithAppleThenDelete() {
        when (auth.reauthenticateWithAppleToRevoke()) {
            AuthResult.Success -> deleteIfSameUser()

            AuthResult.Cancelled -> _state.update { it.copy(busy = false) }

            // Delete without the revocation; an old session still gets the usual NeedsRecentLogin prompt
            else -> deleteAccount()
        }
    }

    override fun reauthWithEmail(
        email: String,
        password: String
    ) = reauth { auth.signInWithEmail(email.trim(), password) }

    override fun reauthWithGoogle() = reauth { auth.signInWithGoogle() }

    override fun reauthWithApple() = reauth { auth.signInWithAppleToRevoke() }

    private fun reauth(signIn: suspend () -> AuthResult) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val result = signIn()) {
                AuthResult.Success -> deleteIfSameUser()
                AuthResult.Cancelled -> _state.update { it.copy(busy = false) }
                AuthResult.NeedsRecentLogin -> _state.update { it.copy(busy = false, error = UiError.UNKNOWN) }
                is AuthResult.Failure -> _state.update { it.copy(busy = false, error = result.reason.toUiError()) }
            }
        }
    }

    // Firebase's own current user, not [AuthService.state], which may lag behind the sign-in
    private suspend fun deleteIfSameUser() {
        if (auth.currentUid() == deletingUid) {
            _state.update { it.copy(needsReauth = false) }
            // The client may still cache the token from before the sign-in; DELETE /me must carry the new one
            tokens.forget()
            deleteAccount()
        } else {
            // Signed in as someone else: that account was not the one confirmed for deletion
            _state.update { it.copy(needsReauth = false, busy = false) }
        }
    }

    private suspend fun deleteAccount() {
        val check = auth.canDeleteNow()
        if (check != AuthResult.Success) return stopWith(check)

        // Once DELETE /me is sent the server data may be gone: the Firebase user must follow it even if the
        // screen is left (the ViewModel's scope cancelled) before the answer arrives
        withContext(NonCancellable) { deleteServerThenFirebaseUser() }
    }

    private suspend fun deleteServerThenFirebaseUser() {
        val server = repository.deleteServerAccount()
        if (server is RestResult.Error) {
            // A 401 ends the session (the sign-in screen explains it); a 426 is covered by the forced-update screen
            if (server.error.isUnauthorized) signOutAndForget(SignOutReason.SESSION_ENDED)
            val silent = server.error.isUnauthorized || server.error.isUpgradeRequired
            val error = if (silent) null else server.error.toUiError()
            return _state.update { it.copy(busy = false, error = error) }
        }

        auth.revokeAppleToken()
        val firebase = auth.deleteUser()
        if (firebase != AuthResult.Success) return stopWith(firebase)
        deletingUid?.let { pendingName.clear(it) }
        // Firebase has signed the user out already; signOut makes it explicit for every listener
        signOutAndForget(SignOutReason.USER)
        _state.update { it.copy(busy = false) }
    }

    private fun stopWith(result: AuthResult) = _state.update {
        when (result) {
            AuthResult.NeedsRecentLogin -> it.copy(busy = false, needsReauth = true)
            is AuthResult.Failure -> it.copy(busy = false, error = result.reason.toUiError())
            AuthResult.Success, AuthResult.Cancelled -> it.copy(busy = false)
        }
    }
}
