package com.kturker.multiplatform.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMappingTest {

    @Test
    fun knownFirebaseCodesMapToFailures() {
        assertEquals(AuthFailure.INVALID_CREDENTIALS, authFailureOf("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthFailure.INVALID_CREDENTIALS, authFailureOf("ERROR_WRONG_PASSWORD"))
        assertEquals(AuthFailure.EMAIL_IN_USE, authFailureOf("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthFailure.WEAK_PASSWORD, authFailureOf("ERROR_WEAK_PASSWORD"))
        assertEquals(AuthFailure.NETWORK, authFailureOf("ERROR_NETWORK_REQUEST_FAILED"))
    }

    @Test
    fun recentLoginIsItsOwnResult() {
        assertEquals(AuthResult.NeedsRecentLogin, authResultOf("ERROR_REQUIRES_RECENT_LOGIN"))
    }

    // A Google or Apple token Firebase refuses (its audience is not one of the project's apps) is not a wrong password
    @Test
    fun aRefusedSocialCredentialIsNotAWrongPassword() {
        assertEquals(AuthResult.Failure(AuthFailure.PROVIDER_REJECTED), socialResultOf("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthResult.Failure(AuthFailure.PROVIDER_REJECTED), socialResultOf("17004"))
        assertEquals(AuthResult.Failure(AuthFailure.NETWORK), socialResultOf("17020"))
        assertEquals(AuthResult.Failure(AuthFailure.EMAIL_IN_USE), socialResultOf("17012"))
    }

    // Firebase refuses another Apple ID and keeps the current user: for the person that is backing out, not an error
    @Test
    fun anotherAccountAtReauthenticationIsACancel() {
        assertEquals(AuthResult.Cancelled, reauthResultOf("ERROR_USER_MISMATCH"))
        assertEquals(AuthResult.Cancelled, reauthResultOf("17024"))
    }

    @Test
    fun otherReauthenticationErrorsMapLikeASocialSignIn() {
        assertEquals(AuthResult.Failure(AuthFailure.PROVIDER_REJECTED), reauthResultOf("ERROR_INVALID_CREDENTIAL"))
        assertEquals(AuthResult.Failure(AuthFailure.NETWORK), reauthResultOf("17020"))
        assertEquals(AuthResult.NeedsRecentLogin, reauthResultOf("17014"))
    }

    @Test
    fun unknownCodesAreUnknown() {
        assertEquals(AuthFailure.UNKNOWN, authFailureOf("SOMETHING_NEW"))
    }

    // GitLive's iOS exceptions carry the numeric FIRAuthErrorCode as their code, not Android's ERROR_* name
    @Test
    fun iosNumericCodesMapLikeTheirAndroidNames() {
        assertEquals(AuthFailure.INVALID_CREDENTIALS, authFailureOf("17004"))
        assertEquals(AuthFailure.INVALID_CREDENTIALS, authFailureOf("17009"))
        assertEquals(AuthFailure.EMAIL_IN_USE, authFailureOf("17007"))
        assertEquals(AuthFailure.WEAK_PASSWORD, authFailureOf("17026"))
        assertEquals(AuthFailure.NETWORK, authFailureOf("17020"))
        assertEquals(AuthResult.NeedsRecentLogin, authResultOf("17014"))
    }

    @Test
    fun aKnownFailureCodeIsAFailureResult() {
        assertEquals(AuthResult.Failure(AuthFailure.EMAIL_IN_USE), authResultOf("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthResult.Failure(AuthFailure.UNKNOWN), authResultOf(null))
    }
}
