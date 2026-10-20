package com.kturker.multiplatform.feature.account.presentation.data

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.api.Profile
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
fun interface ProfileSync {
    fun afterSignIn()
}

/**
 * Drops the cached bearer token, then sends PUT me/profile on [scope], an app-wide one: the sign-in screen, and
 * its ViewModel, closes as soon as the user is signed in. The profile always carries both values the app has now,
 * the sign-in provider's display name (null for an email account) and the personal space's localized name: the
 * backend replaces the display name on each PUT. Once the server has the profile, [selectedSpace] reloads (the app
 * shell's own reload may have answered before the personal space got its localized name), unless another user has
 * signed in meanwhile. A failure only leaves the defaults in place (no display name, the server's space name).
 */
internal class BackendProfileSync(
    private val repository: AccountRepository,
    private val auth: AuthService,
    private val personalSpaceName: PersonalSpaceName,
    private val tokens: SessionTokens,
    private val selectedSpace: SelectedSpace,
    private val scope: CoroutineScope
) : ProfileSync {

    /** The last sync, for tests to wait on. */
    internal var job: Job? = null
        private set

    override fun afterSignIn() {
        // Read now: Firebase's current user is the one who just signed in
        val uid = auth.currentUid()
        val displayName = auth.currentDisplayName()?.trim()?.takeIf { it.isNotEmpty() }
        val profile = Profile(displayName = displayName, personalSpaceName = personalSpaceName.get())
        // The client may still cache the previous user's bearer token: the PUT must carry this user's,
        // or the server would rename the previous user's space
        tokens.forget()
        job = scope.launch {
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
