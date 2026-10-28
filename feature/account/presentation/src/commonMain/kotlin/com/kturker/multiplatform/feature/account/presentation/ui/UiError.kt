package com.kturker.multiplatform.feature.account.presentation.ui

import androidx.compose.runtime.Composable
import com.kturker.multiplatform.core.auth.AuthFailure
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.multilang.resource.AccountStrings
import com.kturker.multiplatform.core.multilang.resource.account

/** What the account screens tell the user; each one has a message in [AccountStrings] ([uiErrorText]). */
internal enum class UiError {
    NETWORK,
    INVALID_CREDENTIALS,
    PROVIDER_REJECTED,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    RATE_LIMITED,
    UNKNOWN
}

private const val UNAUTHORIZED = 401

// The backend's 426 `CLIENT_UPGRADE_REQUIRED`
private const val UPGRADE_REQUIRED = 426

// The backend's 429 `RATE_LIMITED` (DELETE me counts against its strict limiter)
private const val TOO_MANY_REQUESTS = 429

/** A 401 that survived the token refresh: the session is gone and the user has to sign in again. */
internal val DataError.isUnauthorized: Boolean get() = this is DataError.Http && code == UNAUTHORIZED

/** A 426: the backend client has already opened the app-wide forced-update screen, so the screen shows nothing. */
internal val DataError.isUpgradeRequired: Boolean get() = this is DataError.Http && code == UPGRADE_REQUIRED

internal fun DataError.toUiError(): UiError = when {
    this is DataError.Network -> UiError.NETWORK
    this is DataError.Http && code == TOO_MANY_REQUESTS -> UiError.RATE_LIMITED
    else -> UiError.UNKNOWN
}

// Firebase switched the method off, is throttling the device, or has no config: nothing a retry fixes, so the
// general message (the sign-in screen explains a missing config on its own)
internal fun AuthFailure.toUiError(): UiError = when (this) {
    AuthFailure.INVALID_CREDENTIALS -> UiError.INVALID_CREDENTIALS
    AuthFailure.PROVIDER_REJECTED -> UiError.PROVIDER_REJECTED
    AuthFailure.EMAIL_IN_USE -> UiError.EMAIL_IN_USE
    AuthFailure.WEAK_PASSWORD -> UiError.WEAK_PASSWORD
    AuthFailure.NETWORK -> UiError.NETWORK
    AuthFailure.UNAVAILABLE, AuthFailure.UNKNOWN -> UiError.UNKNOWN
}

internal fun UiError.message(strings: AccountStrings): String = when (this) {
    UiError.NETWORK -> strings.errorNetwork
    UiError.INVALID_CREDENTIALS -> strings.errorInvalidCredentials
    UiError.PROVIDER_REJECTED -> strings.errorProviderRejected
    UiError.EMAIL_IN_USE -> strings.errorEmailInUse
    UiError.WEAK_PASSWORD -> strings.errorWeakPassword
    UiError.RATE_LIMITED -> strings.errorRateLimited
    UiError.UNKNOWN -> strings.errorUnknown
}

/** The message for [error], in the app's language (LanguageManager, through LocalStringResources). */
@Composable
internal fun uiErrorText(error: UiError): String =
    error.message(LocalStringResources.current.account)
