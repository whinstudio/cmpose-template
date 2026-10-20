package com.kturker.multiplatform.feature.account.presentation

import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.account.presentation.data.BackendProfileSync
import com.kturker.multiplatform.feature.account.presentation.data.PersonalSpaceName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileSyncTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService()
    private val repository = FakeAccountRepository(log)
    private var spaceName = "Kişisel"

    private fun sync(scope: CoroutineScope) = BackendProfileSync(
        repository = repository,
        auth = auth,
        personalSpaceName = PersonalSpaceName { spaceName },
        tokens = FakeSessionTokens(log),
        selectedSpace = FakeSelectedSpace(log),
        scope = scope
    )

    @Test
    fun aSignInSendsTheFirebaseNameAndTheLocalizedPersonalSpaceName() = runTest {
        auth.displayName = "  Ada Lovelace "
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals(listOf(Profile(displayName = "Ada Lovelace", personalSpaceName = "Kişisel")), repository.profiles)
    }

    // User A signed out, user B signed in: a token cached for A would make the server rename A's space
    @Test
    fun theCachedTokenIsDroppedBeforeTheProfileIsSent() = runTest {
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals(listOf("forgetToken", "updateProfile", "refreshSelectedSpace"), log)
    }

    // The shell's own reload may have answered before the server renamed the personal space: it reloads once the
    // profile is in, so the localized name shows at once
    @Test
    fun theSelectedSpaceReloadsAfterTheProfileIsIn() = runTest {
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals("refreshSelectedSpace", log.last())
        assertEquals(log.indexOf("updateProfile") + 1, log.indexOf("refreshSelectedSpace"))
    }

    @Test
    fun aFailedUpdateDoesNotReloadTheSelectedSpace() = runTest {
        repository.updateProfileResult = RestResult.Error(DataError.Http(500))
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals(listOf("forgetToken", "updateProfile"), log)
    }

    // Signed out (or switched account) before the answer: the reload would belong to someone else
    @Test
    fun noReloadWhenTheUserChangedBeforeTheAnswer() = runTest {
        val sync = sync(this)

        sync.afterSignIn()
        auth.signOut(SignOutReason.USER)
        sync.job?.join()

        assertEquals(listOf("forgetToken", "updateProfile"), log)
    }

    // The app's language at the moment of the sign-in, not the device's
    @Test
    fun thePersonalSpaceNameFollowsTheAppLanguage() = runTest {
        val sync = sync(this)

        spaceName = "Personnel"
        sync.afterSignIn()
        sync.job?.join()

        assertEquals("Personnel", repository.profiles.single().personalSpaceName)
    }

    // Email accounts have no display name: the backend's stays empty (it only ever comes from this sync), and the
    // personal space is still named
    @Test
    fun withoutAFirebaseNameOnlyThePersonalSpaceIsNamed() = runTest {
        auth.displayName = " "
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals(listOf(Profile(displayName = null, personalSpaceName = "Kişisel")), repository.profiles)
    }

    @Test
    fun aFailedUpdateIsOnlyLogged() = runTest {
        repository.updateProfileResult = RestResult.Error(DataError.Network)
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals(1, repository.profiles.size)
    }
}
