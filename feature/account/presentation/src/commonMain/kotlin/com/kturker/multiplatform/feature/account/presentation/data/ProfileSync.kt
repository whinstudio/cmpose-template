package com.kturker.multiplatform.feature.account.presentation.data

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private val log = Logger.withTag("Account")

/** The name of the user's personal space ("Personal") in the app's current language. */
fun interface PersonalSpaceName {
    fun get(): String
}

/** Tells the server who just signed in: their display name and their personal space's name. */
interface ProfileSync {
    fun afterSignIn()

    /** Like [afterSignIn], for an email account just created with [displayName], already trimmed. */
    fun afterSignUp(displayName: String)
}

/**
 * Drops the cached bearer token, then sends PUT me/profile on [scope], an app-wide one: the sign-in screen, and
 * its ViewModel, closes as soon as the user is signed in. The profile always carries both values the app has now,
 * the sign-in provider's display name (null for an email account) and the personal space's localized name: the
 * backend replaces the display name on each PUT. Once the server has the profile, [selectedSpace] reloads (the app
 * shell's own reload may have answered before the personal space got its localized name), unless another user has
 * signed in meanwhile. A failure only leaves the defaults in place (no display name, the server's space name).
 *
 * A sign-up's name is written to the Firebase profile first, on the same scope for the same reason: the screen that
 * asked for it is already gone. It goes to Firebase, not only to the server, because every later sign-in resends
 * Firebase's name and the server would replace its own with nothing. Until Firebase has it, the name waits in
 * [pendingName] under the user's uid: it still reaches the server at once, and the user's next sign-in without a
 * Firebase name writes it to Firebase again and resends it. A stored name rather than retries, since the app may be
 * closed before a retry.
 */
internal class BackendProfileSync(
    private val repository: AccountRepository,
    private val auth: AuthService,
    private val personalSpaceName: PersonalSpaceName,
    private val tokens: SessionTokens,
    private val selectedSpace: SelectedSpace,
    private val pendingName: PendingDisplayName,
    private val scope: CoroutineScope
) : ProfileSync {

    /** The last sync, for tests to wait on. */
    internal var job: Job? = null
        private set

    override fun afterSignIn() {
        // Read now: Firebase's current user is the one who just signed in
        val firebaseName = auth.currentDisplayName()?.trim()?.takeIf { it.isNotEmpty() }
        sync { uid -> signInName(uid, firebaseName) }
    }

    override fun afterSignUp(displayName: String) {
        sync { uid ->
            // Stored before the write: the app may be closed before Firebase answers
            uid?.let { pendingName.put(it, displayName) }
            saveInFirebase(uid, displayName)
            displayName
        }
    }

    private suspend fun signInName(
        uid: String?,
        firebaseName: String?
    ): String? {
        if (uid == null) return firebaseName
        val pending = pendingName.get(uid)
        return when {
            pending == null -> firebaseName

            // Another device may have saved it meanwhile: Firebase's is the one later sign-ins resend
            firebaseName != null -> firebaseName.also { pendingName.clear(uid) }

            else -> pending.also { saveInFirebase(uid, it) }
        }
    }

    private suspend fun saveInFirebase(
        uid: String?,
        name: String
    ) {
        // Nobody signed in: no one to write it to
        if (uid == null) return
        // Written only while [uid] is still the signed-in user: another account signed in meanwhile gets no name
        val saved = auth.setDisplayName(uid, name)
        if (saved == AuthResult.Success) {
            pendingName.clear(uid)
        } else {
            log.w { "Display name not saved in Firebase: $saved" }
        }
    }

    /** Sends the profile with the name [displayName] picks for the user signed in now (their uid, if any). */
    private fun sync(displayName: suspend (uid: String?) -> String?) {
        val uid = auth.currentUid()
        val spaceName = personalSpaceName.get()
        // The client may still cache the previous user's bearer token: the PUT must carry this user's,
        // or the server would rename the previous user's space
        tokens.forget()
        job = scope.launch {
            val profile = Profile(displayName = displayName(uid), personalSpaceName = spaceName)
            val result = repository.updateProfile(profile)
            if (result is RestResult.Error) {
                val error = result.error
                log.w { "Profile not updated: ${(error as? DataError.Http)?.code ?: error::class.simpleName}" }
            } else if (auth.currentUid() == uid) {
                selectedSpace.refresh()
            }
        }
    }
}
