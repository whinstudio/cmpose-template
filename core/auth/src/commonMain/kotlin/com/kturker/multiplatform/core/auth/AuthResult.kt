package com.kturker.multiplatform.core.auth

sealed interface AuthResult {
    data object Success : AuthResult
    data object Cancelled : AuthResult

    /** Firebase refused a sensitive action on an old session (account deletion). */
    data object NeedsRecentLogin : AuthResult
    data class Failure(val reason: AuthFailure) : AuthResult
}

/** [PROVIDER_REJECTED]: Firebase refused a Google or Apple account (e.g. a token for an app the project lacks). */
enum class AuthFailure {
    INVALID_CREDENTIALS,
    PROVIDER_REJECTED,
    EMAIL_IN_USE,
    WEAK_PASSWORD,
    NETWORK,
    UNAVAILABLE,
    UNKNOWN
}

/**
 * Maps a Firebase Auth error code to a failure. Android reports the SDK's ERROR_* names; GitLive's iOS
 * exceptions carry the numeric FIRAuthErrorCode as a string, so both spellings are listed.
 */
internal fun authFailureOf(code: String?): AuthFailure = when (code) {
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_EMAIL",
    "ERROR_USER_DISABLED", "ERROR_USER_MISMATCH",
    "17004", "17009", "17011", "17008", "17005", "17024"
    -> AuthFailure.INVALID_CREDENTIALS

    "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE",
    "17007", "17012", "17025"
    -> AuthFailure.EMAIL_IN_USE

    "ERROR_WEAK_PASSWORD", "17026" -> AuthFailure.WEAK_PASSWORD

    "ERROR_NETWORK_REQUEST_FAILED", "17020" -> AuthFailure.NETWORK

    // The sign-in method is switched off in the Firebase console, or Firebase is throttling the device
    "ERROR_OPERATION_NOT_ALLOWED", "ERROR_TOO_MANY_REQUESTS", "17006", "17010" -> AuthFailure.UNAVAILABLE

    else -> AuthFailure.UNKNOWN
}

internal fun authResultOf(code: String?): AuthResult = when (code) {
    "ERROR_REQUIRES_RECENT_LOGIN", "17014" -> AuthResult.NeedsRecentLogin
    else -> AuthResult.Failure(authFailureOf(code))
}

/** Firebase's answer to a Google or Apple credential: a refused one is the account, not a typed password. */
internal fun socialResultOf(code: String?): AuthResult {
    val result = authResultOf(code)
    return if (result == AuthResult.Failure(AuthFailure.INVALID_CREDENTIALS)) {
        AuthResult.Failure(AuthFailure.PROVIDER_REJECTED)
    } else {
        result
    }
}

/** Firebase's answer to reauthenticating with a social account: another account keeps the user and backs out. */
internal fun reauthResultOf(code: String?): AuthResult = when (code) {
    "ERROR_USER_MISMATCH", "17024" -> AuthResult.Cancelled
    else -> socialResultOf(code)
}
