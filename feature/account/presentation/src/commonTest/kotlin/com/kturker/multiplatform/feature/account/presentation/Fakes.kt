package com.kturker.multiplatform.feature.account.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.AppVersion
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.auth.AuthFailure
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionNotice
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.feature.account.presentation.data.AccountRepository
import com.kturker.multiplatform.feature.account.presentation.data.PendingDisplayName
import com.kturker.multiplatform.feature.account.presentation.data.ProfileSync
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Results are queues: each call takes the next one, and the last one repeats. Every call is appended to
 * [log], which the repository fake can share to show the order across both.
 */
internal class FakeAuthService(
    val log: MutableList<String> = mutableListOf(),
    initial: AuthState = AuthState.SignedIn("u1", "me@example.com"),
) : AuthService {
    override val state = MutableStateFlow(initial)

    /** Each sign-out's reason, in order; like the real service, SESSION_ENDED posts the one-shot notice. */
    val signOutReasons = mutableListOf<SignOutReason>()
    private val notice = SessionNotice()

    val emailSignIn = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val emailSignUp = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val google = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val apple = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val deleteUserResults = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val canDeleteNowResults = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val appleToRevoke = ArrayDeque(listOf<AuthResult>(AuthResult.Success))
    val appleReauth = ArrayDeque(listOf<AuthResult>(AuthResult.Success))

    /** Whether the signed-in user has an Apple sign-in. */
    var usesApple = false

    /** The uid a successful sign-in leaves signed in; another account's uid simulates a switch. */
    var uidAfterSignIn = "u1"

    /** The display name Firebase reports for the current user. */
    var displayName: String? = null

    /**
     * What writing a display name to Firebase answers; a success also makes it [currentDisplayName]. Like the real
     * service, a write for a uid that is not [currentUid] writes nothing and answers [AuthFailure.UNKNOWN].
     */
    var setDisplayNameResult: AuthResult = AuthResult.Success

    /**
     * Like the real service, whose [state] follows Firebase's listener: when true, a sign-in changes
     * [currentUid] at once but leaves [state] on the previous user.
     */
    var stateLags = false

    private var firebaseUid: String? = (initial as? AuthState.SignedIn)?.uid

    override fun currentUid(): String? = firebaseUid

    override fun currentDisplayName(): String? = displayName

    private fun ArrayDeque<AuthResult>.next(): AuthResult = if (size > 1) removeFirst() else first()

    private fun signedIn(result: AuthResult): AuthResult {
        if (result == AuthResult.Success) {
            firebaseUid = uidAfterSignIn
            if (!stateLags) state.value = AuthState.SignedIn(uidAfterSignIn, "me@example.com")
        }
        return result
    }

    override suspend fun token(forceRefresh: Boolean): String? = "token"

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        log += "signInWithEmail:$email"
        return signedIn(emailSignIn.next())
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        log += "signUpWithEmail:$email"
        return signedIn(emailSignUp.next())
    }

    override suspend fun setDisplayName(uid: String, name: String): AuthResult {
        if (uid != firebaseUid) return AuthResult.Failure(AuthFailure.UNKNOWN)
        log += "setDisplayName:$name"
        if (setDisplayNameResult == AuthResult.Success) displayName = name
        return setDisplayNameResult
    }

    override suspend fun signInWithGoogle(): AuthResult {
        log += "signInWithGoogle"
        return signedIn(google.next())
    }

    override suspend fun signInWithApple(): AuthResult {
        log += "signInWithApple"
        return signedIn(apple.next())
    }

    override suspend fun signOut(reason: SignOutReason) {
        log += "signOut"
        signOutReasons += reason
        if (reason == SignOutReason.SESSION_ENDED) notice.post()
        firebaseUid = null
        state.value = AuthState.SignedOut
    }

    override fun takeSessionEndedNotice(): Boolean = notice.take()

    override suspend fun deleteUser(): AuthResult {
        log += "deleteUser"
        val result = deleteUserResults.next()
        if (result == AuthResult.Success) {
            firebaseUid = null
            state.value = AuthState.SignedOut
        }
        return result
    }

    override suspend fun canDeleteNow(): AuthResult {
        log += "canDeleteNow"
        return canDeleteNowResults.next()
    }

    override fun usesAppleSignIn(): Boolean = usesApple

    override suspend fun signInWithAppleToRevoke(): AuthResult {
        log += "signInWithAppleToRevoke"
        return signedIn(appleToRevoke.next())
    }

    override suspend fun reauthenticateWithAppleToRevoke(): AuthResult {
        log += "reauthenticateWithAppleToRevoke"
        return signedIn(appleReauth.next())
    }

    /** When set, revoking the Apple token waits for it: the first step after DELETE /me has answered. */
    var revokeGate: CompletableDeferred<Unit>? = null

    // Logged only for Apple users, so the call orders non-Apple tests assert stay as they are
    override suspend fun revokeAppleToken() {
        if (usesApple) log += "revokeAppleToken"
        revokeGate?.await()
    }
}

internal class FakeAccountRepository(val log: MutableList<String> = mutableListOf()) : AccountRepository {
    var deleteServerAccountResult: RestResult<Unit> = RestResult.Success(Unit)
    var spacesResult: RestResult<List<Space>> = RestResult.Success(emptyList())
    var updateProfileResult: RestResult<Unit> = RestResult.Success(Unit)
    val profiles = mutableListOf<Profile>()

    /** When set, GET /spaces waits for it before answering. */
    var spacesGate: CompletableDeferred<Unit>? = null

    override suspend fun spaces(): RestResult<List<Space>> {
        log += "spaces"
        spacesGate?.await()
        return spacesResult
    }

    override suspend fun updateProfile(profile: Profile): RestResult<Unit> {
        log += "updateProfile"
        profiles += profile
        return updateProfileResult
    }

    /** When set, DELETE /me waits for it before answering. */
    var deleteServerAccountGate: CompletableDeferred<Unit>? = null

    override suspend fun deleteServerAccount(): RestResult<Unit> {
        log += "deleteServerAccount"
        deleteServerAccountGate?.await()
        return deleteServerAccountResult
    }
}

/** Appends "forgetToken" to [log] each time the cached bearer token is dropped. */
internal class FakeSessionTokens(private val log: MutableList<String>) : SessionTokens {
    override fun forget() {
        log += "forgetToken"
    }
}

/** Counts the sign-ins it was told about ([calls]) and keeps each sign-up's name ([signUps]). */
internal class FakeProfileSync : ProfileSync {
    var calls = 0
        private set

    val signUps = mutableListOf<String>()

    override fun afterSignIn() {
        calls++
    }

    override fun afterSignUp(displayName: String) {
        signUps += displayName
    }
}

internal class RecordingNavigationManager : NavigationManager {
    val commands = mutableListOf<NavigationCommand>()
    override val navigationCommandFlow: Flow<NavigationCommand> = emptyFlow()

    override fun navigate(navigationCommand: NavigationCommand) {
        commands += navigationCommand
    }
}

/** Records each [refresh] in [log]; the account screens only ever reload it. */
internal class FakeSelectedSpace(val log: MutableList<String> = mutableListOf()) : SelectedSpace {
    override val current = MutableStateFlow<Space?>(null)

    override suspend fun select(spaceId: String) {
        log += "select:$spaceId"
    }

    override suspend fun refresh() {
        log += "refreshSelectedSpace"
    }

    override suspend fun clear() {
        log += "clearSelectedSpace"
    }
}

/** Keeps the names in [names]; appends each put and clear to [log] (reads are not logged). */
internal class FakePendingDisplayName(private val log: MutableList<String> = mutableListOf()) : PendingDisplayName {
    val names = mutableMapOf<String, String>()

    override suspend fun put(uid: String, name: String) {
        log += "putPendingName:$uid:$name"
        names[uid] = name
    }

    override suspend fun get(uid: String): String? = names[uid]

    override suspend fun clear(uid: String) {
        log += "clearPendingName:$uid"
        names.remove(uid)
    }
}

/** A DataStore held in memory, for the stores built on one. */
internal class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: StateFlow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}

/** The app's version and legal links, as the account screens read them; no links by default. */
internal class FakeAppInfo(override val legalLinks: List<LegalLink> = emptyList()) : AppInfo {
    override val version = AppVersion(name = "1.0.0", code = "1")
}

internal val PRIVACY_LINK = LegalLink(kind = LegalLinkKind.PrivacyPolicy, url = "https://example.com/privacy")
internal val TERMS_LINK = LegalLink(kind = LegalLinkKind.Terms, url = "https://example.com/terms")
