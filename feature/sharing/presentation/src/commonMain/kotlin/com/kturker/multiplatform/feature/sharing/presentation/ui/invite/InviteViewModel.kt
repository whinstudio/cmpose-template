package com.kturker.multiplatform.feature.sharing.presentation.ui.invite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.api.InviteState
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.auth.signOutAndForget
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.account.contract.LoginDestination
import com.kturker.multiplatform.feature.sharing.contract.InviteDestination
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.PendingInviteStore
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.ui.HTTP_NOT_FOUND
import com.kturker.multiplatform.feature.sharing.presentation.ui.HTTP_TOO_MANY_REQUESTS
import com.kturker.multiplatform.feature.sharing.presentation.ui.HTTP_UNAUTHORIZED
import com.kturker.multiplatform.feature.sharing.presentation.ui.HTTP_UPGRADE_REQUIRED
import com.kturker.multiplatform.feature.sharing.presentation.ui.httpCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

// The backend's INVITE_USED and INVITE_EXPIRED
private const val HTTP_CONFLICT = 409
private const val HTTP_GONE = 410

/**
 * An invite link's preview and acceptance. Opened while nobody is signed in, it keeps the token in
 * [PendingInviteStore] and opens the sign-in; the app shell takes the token after the sign-in and opens this
 * screen again. Accepting selects the joined space and goes back to the app.
 */
@KoinViewModel
internal class InviteViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SpacesRepository,
    @Provided private val auth: AuthService,
    @Provided private val tokens: SessionTokens,
    private val pendingInvites: PendingInviteStore,
    private val selectedSpace: SelectedSpace,
    @Provided private val navigationManager: NavigationManager
) : CoreViewModel(),
    InviteActions {

    // Read by its name, like MembersViewModel's spaceId
    private val token: String = checkNotNull(savedStateHandle[InviteDestination::token.name]) { "token missing" }

    private val _state = MutableStateFlow(InviteUiState())
    val state: StateFlow<InviteUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // A cold start from the link: wait until Firebase has reported the restored session
            val session = auth.state.first { it != AuthState.Loading }
            if (session is AuthState.SignedIn) load() else waitForSignIn()
        }
    }

    override fun accept() {
        if (_state.value.busy || !_state.value.canAccept) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = repository.accept(token)) {
                is RestResult.Success -> {
                    selectedSpace.select(result.result.id)
                    navigationManager.navigate(NavigationCommand.NavigateUp)
                }

                is RestResult.Error -> fail(result.error)

                is RestResult.Loading -> Unit
            }
        }
    }

    override fun close() = navigationManager.navigate(NavigationCommand.NavigateUp)

    override fun retry() {
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        when (val result = repository.preview(token)) {
            is RestResult.Success -> {
                val preview = result.result
                val message = when (preview.state) {
                    InviteState.VALID -> null
                    InviteState.USED -> InviteMessage.USED
                    InviteState.EXPIRED -> InviteMessage.EXPIRED
                }
                _state.update { it.copy(loading = false, preview = preview, message = message) }
            }

            is RestResult.Error -> fail(result.error)

            is RestResult.Loading -> Unit
        }
    }

    // Signed out (or the app has no Firebase config): the invite waits, and the sign-in screen opens on its own
    private suspend fun waitForSignIn() {
        pendingInvites.put(token)
        navigationManager.navigate(NavigationCommand.NavigateTo(LoginDestination, clearBackStack = true))
    }

    private suspend fun fail(error: DataError) {
        if (error.httpCode == HTTP_UNAUTHORIZED) {
            // The session ended: the account rule, and the invite waits for the sign-in that follows
            pendingInvites.put(token)
            auth.signOutAndForget(tokens, SignOutReason.SESSION_ENDED)
            return
        }
        // A 426 (upgrade required) gets no message: the forced-update screen covers the app
        val message = when {
            error.httpCode == HTTP_UPGRADE_REQUIRED -> null
            error.httpCode == HTTP_CONFLICT -> InviteMessage.USED
            error.httpCode == HTTP_GONE -> InviteMessage.EXPIRED
            error.httpCode == HTTP_NOT_FOUND -> InviteMessage.NOT_FOUND
            error.httpCode == HTTP_TOO_MANY_REQUESTS -> InviteMessage.RATE_LIMITED
            error is DataError.Network -> InviteMessage.NETWORK
            else -> InviteMessage.UNKNOWN
        }
        _state.update { it.copy(loading = false, busy = false, message = message) }
    }
}
