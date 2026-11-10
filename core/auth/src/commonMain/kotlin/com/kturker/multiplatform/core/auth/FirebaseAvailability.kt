package com.kturker.multiplatform.core.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the app carries its Firebase config: `google-services.json` on Android (applied by the
 * google-services plugin), `GoogleService-Info.plist` on iOS (read by `FirebaseApp.configure()`). Both are
 * per developer and git-ignored, so a fresh checkout builds and runs without them.
 */
interface FirebaseAvailability {
    val isConfigured: Boolean
}

/** Android: the Firebase app or its resources; iOS: what the Swift app reported (IosSignInBridge). */
internal expect fun platformFirebaseAvailability(): FirebaseAvailability

/**
 * The Firebase-backed service when Firebase is configured, otherwise one that is always [AuthState.Unavailable].
 * [connect] is not called without a config: Firebase's SDK throws on first use when it has no app.
 */
internal fun authServiceFor(
    firebase: FirebaseAvailability,
    connect: () -> AuthService
): AuthService = if (firebase.isConfigured) connect() else UnavailableAuthService()

/** No Firebase config: nobody can sign in, there is no token, and every call answers UNAVAILABLE. */
internal class UnavailableAuthService : AuthService {

    override val state: StateFlow<AuthState> = MutableStateFlow(AuthState.Unavailable).asStateFlow()

    override fun currentUid(): String? = null

    override fun currentDisplayName(): String? = null

    override suspend fun token(forceRefresh: Boolean): String? = null

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthResult = UNAVAILABLE

    override suspend fun signUpWithEmail(
        email: String,
        password: String
    ): AuthResult = UNAVAILABLE

    override suspend fun setDisplayName(
        uid: String,
        name: String
    ): AuthResult = UNAVAILABLE

    override suspend fun signInWithGoogle(): AuthResult = UNAVAILABLE

    override suspend fun signInWithApple(): AuthResult = UNAVAILABLE

    override suspend fun signOut(reason: SignOutReason) = Unit

    override fun takeSessionEndedNotice(): Boolean = false

    override suspend fun deleteUser(): AuthResult = UNAVAILABLE

    override suspend fun canDeleteNow(): AuthResult = UNAVAILABLE

    override fun usesAppleSignIn(): Boolean = false

    override suspend fun signInWithAppleToRevoke(): AuthResult = UNAVAILABLE

    override suspend fun reauthenticateWithAppleToRevoke(): AuthResult = UNAVAILABLE

    override suspend fun revokeAppleToken() = Unit

    private companion object {
        val UNAVAILABLE = AuthResult.Failure(AuthFailure.UNAVAILABLE)
    }
}
