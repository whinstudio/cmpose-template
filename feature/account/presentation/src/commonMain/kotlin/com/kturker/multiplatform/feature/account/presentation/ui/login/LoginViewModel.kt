package com.kturker.multiplatform.feature.account.presentation.ui.login

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.account.presentation.data.ProfileSync
import com.kturker.multiplatform.feature.account.presentation.ui.UiError
import com.kturker.multiplatform.feature.account.presentation.ui.toUiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/**
 * Signs in or up; the app shell leaves the sign-in screen once [AuthService] reports the user. A successful
 * sign-in tells the server the display name and the personal space's localized name ([ProfileSync]).
 */
@KoinViewModel
internal class LoginViewModel(
    @Provided private val auth: AuthService,
    private val profileSync: ProfileSync,
    @Provided appInfo: AppInfo
) : CoreViewModel(),
    LoginActions {

    // The notice is taken once: it belongs to the sign-in screen right after the sign-out, not to later ones
    private val _state = MutableStateFlow(
        LoginUiState(
            sessionEnded = auth.takeSessionEndedNotice(),
            firebaseUnavailable = auth.state.value == AuthState.Unavailable,
            legalLinks = appInfo.legalLinks
        )
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    override fun onEmailChange(email: String) = _state.update { it.copy(email = email.trim()) }

    override fun onPasswordChange(password: String) = _state.update { it.copy(password = password) }

    override fun onDisplayNameChange(name: String) = _state.update {
        val cut = name.take(MAX_DISPLAY_NAME_LENGTH)
        // A cut through an emoji leaves half a surrogate pair, which is not valid text
        val whole = if (cut.lastOrNull()?.isHighSurrogate() == true) cut.dropLast(1) else cut
        it.copy(displayName = whole)
    }

    override fun startCreatingAccount() =
        _state.update { it.copy(creatingAccount = true, error = null) }

    override fun backToSignIn() {
        // The system back gesture also lands here: a sign-up's answer belongs to the form that sent it
        if (_state.value.busy) return
        _state.update { it.copy(creatingAccount = false, error = null) }
    }

    override fun signIn() =
        attempt { auth.signInWithEmail(state.value.email, state.value.password) }

    override fun signUp() {
        if (!_state.value.canCreateAccount) return
        val name = _state.value.displayName.trim()
        // ProfileSync writes the name: this screen closes as soon as the account exists
        attempt(onSuccess = { profileSync.afterSignUp(name) }) {
            auth.signUpWithEmail(state.value.email, state.value.password)
        }
    }

    override fun signInWithGoogle() = attempt { auth.signInWithGoogle() }

    override fun signInWithApple() = attempt { auth.signInWithApple() }

    private fun attempt(
        onSuccess: () -> Unit = profileSync::afterSignIn,
        call: suspend () -> AuthResult
    ) {
        // Without a Firebase config the buttons are off; a call that still arrives does nothing
        if (!_state.value.signInEnabled) return
        _state.update { it.copy(busy = true, error = null, sessionEnded = false) }
        viewModelScope.launch {
            val result = call()
            if (result == AuthResult.Success) onSuccess()
            val error = when (result) {
                is AuthResult.Failure -> result.reason.toUiError()

                // Closing the Google or Apple sheet is not an error
                AuthResult.Success, AuthResult.Cancelled -> null

                AuthResult.NeedsRecentLogin -> UiError.UNKNOWN
            }
            _state.update { it.copy(busy = false, error = error) }
        }
    }
}
