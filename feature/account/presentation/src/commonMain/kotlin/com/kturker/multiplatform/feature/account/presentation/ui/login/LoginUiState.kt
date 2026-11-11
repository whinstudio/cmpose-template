package com.kturker.multiplatform.feature.account.presentation.ui.login

import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.feature.account.presentation.ui.UiError

/** The server's limit on a display name; a longer one is refused. */
internal const val MAX_DISPLAY_NAME_LENGTH = 80

internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    /** Kept as typed, spaces included; trimmed once, when the account is created. */
    val displayName: String = "",
    /** The create-account form is shown instead of the sign-in one. */
    val creatingAccount: Boolean = false,
    val busy: Boolean = false,
    val error: UiError? = null,
    /** The previous session ended on the server's 401, not by the user's choice; cleared by the next attempt. */
    val sessionEnded: Boolean = false,
    /** The app has no Firebase config: the screen says so and every sign-in method is off. */
    val firebaseUnavailable: Boolean = false,
    /** The privacy policy and terms below the form; empty while their URLs are (core/about LegalUrls.kt). */
    val legalLinks: List<LegalLink> = emptyList()
) {
    val signInEnabled: Boolean get() = !busy && !firebaseUnavailable

    val canCreateAccount: Boolean get() = signInEnabled && displayName.isNotBlank()
}
