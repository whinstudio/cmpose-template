package com.kturker.multiplatform.feature.account.presentation

import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.auth.AuthFailure
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.feature.account.presentation.data.BackendProfileSync
import com.kturker.multiplatform.feature.account.presentation.data.PersonalSpaceName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileSyncTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log = log)
    private val repository = FakeAccountRepository(log)
    private var spaceName = "Kişisel"
    private val pending = FakePendingDisplayName(log)

    private fun sync(scope: CoroutineScope) = BackendProfileSync(
        repository = repository,
        auth = auth,
        personalSpaceName = PersonalSpaceName { spaceName },
        tokens = FakeSessionTokens(log),
        selectedSpace = FakeSelectedSpace(log),
        pendingName = pending,
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

        assertEquals(listOf("forgetToken", "signOut", "updateProfile"), log)
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

    // The sign-in screen closes as soon as the account exists: the name is written here, on the app-wide scope
    @Test
    fun aSignUpSavesTheNameInFirebaseThenSendsIt() = runTest {
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()

        assertEquals(
            listOf(
                "forgetToken",
                "putPendingName:u1:Ada",
                "setDisplayName:Ada",
                "clearPendingName:u1",
                "updateProfile",
                "refreshSelectedSpace"
            ),
            log
        )
        assertEquals(listOf(Profile(displayName = "Ada", personalSpaceName = "Kişisel")), repository.profiles)
    }

    @Test
    fun aFailedFirebaseWriteStillSendsTheName() = runTest {
        auth.setDisplayNameResult = AuthResult.Failure(AuthFailure.NETWORK)
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()

        assertEquals(listOf(Profile(displayName = "Ada", personalSpaceName = "Kişisel")), repository.profiles)
    }

    // Each sign-in resends Firebase's name and the server replaces its own: the name has to live in Firebase
    @Test
    fun aLaterSignInKeepsTheName() = runTest {
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()
        sync.afterSignIn()
        sync.job?.join()

        assertEquals("Ada", repository.profiles.last().displayName)
    }

    // Offline right after the account was created: the name waits on the device for this user's next sign-in
    @Test
    fun aFailedFirebaseWriteKeepsTheNameForLater() = runTest {
        auth.setDisplayNameResult = AuthResult.Failure(AuthFailure.NETWORK)
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()

        assertEquals(mapOf("u1" to "Ada"), pending.names)
        assertEquals("Ada", repository.profiles.single().displayName)
    }

    @Test
    fun theNextSignInSendsThePendingName() = runTest {
        auth.setDisplayNameResult = AuthResult.Failure(AuthFailure.NETWORK)
        val sync = sync(this)
        sync.afterSignUp("Ada")
        sync.job?.join()

        auth.setDisplayNameResult = AuthResult.Success
        log.clear()
        sync.afterSignIn()
        sync.job?.join()

        assertEquals("Ada", auth.displayName)
        assertTrue("setDisplayName:Ada" in log)
        assertEquals("Ada", repository.profiles.last().displayName)
        assertTrue(pending.names.isEmpty())
    }

    @Test
    fun aSavedNameClearsThePendingOne() = runTest {
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()

        assertTrue(pending.names.isEmpty())
    }

    // Two accounts on one device: a name left by one is never written to the other's profile
    @Test
    fun anotherUserDoesNotGetThePendingName() = runTest {
        pending.names["u1"] = "Ada"
        auth.signOut(SignOutReason.USER)
        auth.uidAfterSignIn = "u2"
        auth.signInWithEmail("grace@example.com", "secret")
        log.clear()
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertNull(repository.profiles.single().displayName)
        assertFalse(log.any { it.startsWith("setDisplayName") })
        assertEquals(mapOf("u1" to "Ada"), pending.names)
    }

    // Firebase got a name some other way (another device): that is the one every later sign-in resends
    @Test
    fun firebasesNameWinsOverThePendingOne() = runTest {
        pending.names["u1"] = "Ada"
        auth.displayName = "Ada L."
        val sync = sync(this)

        sync.afterSignIn()
        sync.job?.join()

        assertEquals("Ada L.", repository.profiles.single().displayName)
        assertFalse(log.any { it.startsWith("setDisplayName") })
        assertTrue(pending.names.isEmpty())
    }

    // Another account signed in before the write ran: the sign-up's name is never written to that account
    @Test
    fun aUserSwitchBeforeTheWriteGivesTheNewUserNoName() = runTest {
        val sync = sync(this)

        sync.afterSignUp("Ada")
        auth.signOut(SignOutReason.USER)
        auth.uidAfterSignIn = "u2"
        auth.signInWithEmail("grace@example.com", "secret")
        sync.job?.join()

        assertNull(auth.displayName)
        assertFalse(log.any { it.startsWith("setDisplayName") })
        assertEquals(mapOf("u1" to "Ada"), pending.names)
    }

    // Nobody signed in: there is no one to write the name to
    @Test
    fun withoutAUserTheNameIsNotWritten() = runTest {
        auth.signOut(SignOutReason.USER)
        log.clear()
        val sync = sync(this)

        sync.afterSignUp("Ada")
        sync.job?.join()

        assertFalse(log.any { it.startsWith("setDisplayName") })
        assertNull(auth.displayName)
        assertTrue(pending.names.isEmpty())
    }
}
