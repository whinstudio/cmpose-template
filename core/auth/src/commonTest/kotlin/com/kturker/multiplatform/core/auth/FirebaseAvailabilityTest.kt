package com.kturker.multiplatform.core.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Without the Firebase config files (google-services.json, GoogleService-Info.plist) the app still starts:
 * the auth service reports Unavailable and never touches Firebase, whose SDK would throw on first use.
 */
class FirebaseAvailabilityTest {

    private object NotConfigured : FirebaseAvailability {
        override val isConfigured = false
    }

    private object Configured : FirebaseAvailability {
        override val isConfigured = true
    }

    private val firebaseMustNotBeTouched: () -> AuthService = { error("Firebase must not be touched") }

    @Test
    fun withoutAConfigTheStateIsUnavailable() {
        val auth = authServiceFor(NotConfigured, firebaseMustNotBeTouched)

        assertEquals(AuthState.Unavailable, auth.state.value)
        assertNull(auth.currentUid())
    }

    @Test
    fun withoutAConfigEverySignInFailsAsUnavailable() = runTest {
        val auth = authServiceFor(NotConfigured, firebaseMustNotBeTouched)
        val unavailable = AuthResult.Failure(AuthFailure.UNAVAILABLE)

        assertEquals(unavailable, auth.signInWithEmail("user@example.com", "secret-password"))
        assertEquals(unavailable, auth.signUpWithEmail("user@example.com", "secret-password"))
        assertEquals(unavailable, auth.setDisplayName("u1", "Ada"))
        assertEquals(unavailable, auth.signInWithGoogle())
        assertEquals(unavailable, auth.signInWithApple())
    }

    @Test
    fun withoutAConfigThereIsNoAppleTokenToRevoke() = runTest {
        val auth = authServiceFor(NotConfigured, firebaseMustNotBeTouched)
        val unavailable = AuthResult.Failure(AuthFailure.UNAVAILABLE)

        assertFalse(auth.usesAppleSignIn())
        assertEquals(unavailable, auth.signInWithAppleToRevoke())
        assertEquals(unavailable, auth.reauthenticateWithAppleToRevoke())
        auth.revokeAppleToken()
    }

    @Test
    fun withoutAConfigThereIsNoTokenAndNothingToDelete() = runTest {
        val auth = authServiceFor(NotConfigured, firebaseMustNotBeTouched)

        assertNull(auth.token(forceRefresh = false))
        assertNull(auth.token(forceRefresh = true))
        assertEquals(AuthResult.Failure(AuthFailure.UNAVAILABLE), auth.deleteUser())
        assertEquals(AuthResult.Failure(AuthFailure.UNAVAILABLE), auth.canDeleteNow())
        auth.signOut(SignOutReason.SESSION_ENDED)
        assertFalse(auth.takeSessionEndedNotice())
    }

    @Test
    fun withAConfigTheFirebaseServiceIsUsed() {
        // Stands in for the Firebase-backed service, which needs a real Firebase app
        val firebase: AuthService = UnavailableAuthService()

        assertSame(firebase, authServiceFor(Configured) { firebase })
    }
}
