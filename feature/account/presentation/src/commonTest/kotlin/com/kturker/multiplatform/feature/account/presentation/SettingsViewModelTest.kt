package com.kturker.multiplatform.feature.account.presentation

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.about.legalLinks
import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.auth.AuthFailure
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.feature.account.presentation.data.DataStorePendingDisplayName
import com.kturker.multiplatform.feature.account.presentation.data.sharedOwnedCount
import com.kturker.multiplatform.feature.account.presentation.ui.UiError
import com.kturker.multiplatform.feature.account.presentation.ui.settings.SettingsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log)
    private val repository = FakeAccountRepository(log)
    private val tokens = FakeSessionTokens(log)
    private val navigation = RecordingNavigationManager()
    private val preferences = InMemoryPreferences()
    private val pendingName = DataStorePendingDisplayName(preferences)
    private val appInfo = FakeAppInfo()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SettingsViewModel(repository, auth, tokens, navigation, pendingName, appInfo)

    private fun TestScope.confirmedDeletion() = viewModel().apply {
        requestDelete()
        // Delete is enabled once the shared-space count has arrived
        advanceUntilIdle()
        confirmDelete()
    }

    private fun space(
        id: String,
        role: Role = Role.OWNER,
        memberCount: Int = 2,
        personal: Boolean = false,
    ) = Space(id = id, name = id, role = role, personal = personal, memberCount = memberCount)

    @Test
    fun showsTheSignedInEmail() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("me@example.com", viewModel.state.value.email)
    }

    // The stores ask for the privacy policy next to the account deletion
    @Test
    fun showsTheLegalLinksThatAreSet() = runTest {
        val appInfo = FakeAppInfo(listOf(PRIVACY_LINK, TERMS_LINK))
        val viewModel = SettingsViewModel(repository, auth, tokens, navigation, pendingName, appInfo)

        assertEquals(listOf(PRIVACY_LINK, TERMS_LINK), viewModel.state.value.legalLinks)
    }

    @Test
    fun hidesTheLegalLinksWhileTheUrlsAreEmpty() = runTest {
        val appInfo = FakeAppInfo(legalLinks(privacyPolicyUrl = "", termsUrl = ""))
        val viewModel = SettingsViewModel(repository, auth, tokens, navigation, pendingName, appInfo)

        assertEquals(emptyList(), viewModel.state.value.legalLinks)
    }

    @Test
    fun backNavigatesUp() = runTest {
        viewModel().onBack()

        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigation.commands)
    }

    // Leaving mid-deletion would clear the ViewModel between DELETE /me and the Firebase deletion
    @Test
    fun backIsIgnoredWhileDeleting() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.deleteServerAccountGate = gate
        val viewModel = confirmedDeletion()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.busy)

        viewModel.onBack()

        assertEquals(emptyList<NavigationCommand>(), navigation.commands)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    // With the server data gone, a Firebase user left behind could sign in to an account the server no longer has
    @Test
    fun theFirebaseUserIsDeletedEvenWhenTheScreenGoes() = runTest {
        auth.usesApple = true
        val gate = CompletableDeferred<Unit>()
        auth.revokeGate = gate
        val viewModel = confirmedDeletion()
        advanceUntilIdle()
        assertEquals("revokeAppleToken", log.last())

        viewModel.viewModelScope.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            listOf("revokeAppleToken", "deleteUser", "signOut", "forgetToken"),
            log.takeLast(4)
        )
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun deleteAsksForConfirmationFirst() = runTest {
        val viewModel = viewModel()

        viewModel.requestDelete()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.confirmingDelete)
        // Only the warning's lookup: nothing is deleted before the confirmation
        assertEquals(listOf("spaces"), log)

        viewModel.dismissDelete()
        assertFalse(viewModel.state.value.confirmingDelete)
    }

    // Every owned space with other members goes away for someone else, the personal space included once shared
    @Test
    fun deleteWarningCountsSharedOwnedSpaces() = runTest {
        repository.spacesResult = RestResult.Success(
            listOf(
                space("personal-shared", memberCount = 2, personal = true),
                space("shared-a", memberCount = 3),
                space("shared-b", memberCount = 2),
                space("owned-alone", memberCount = 1),
                space("someone-elses", role = Role.EDITOR, memberCount = 4),
                space("viewed", role = Role.VIEWER, memberCount = 2),
            )
        )
        val viewModel = viewModel()

        viewModel.requestDelete()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.confirmingDelete)
        assertEquals(3, viewModel.state.value.sharedSpacesToDelete)
    }

    // role x members x personal: only an owner's space with someone else in it counts
    @Test
    fun sharedOwnedCountTruthTable() {
        val cases = listOf(
            space("owner-personal-alone", memberCount = 1, personal = true) to 0,
            space("owner-personal-shared", memberCount = 2, personal = true) to 1,
            space("owner-alone", memberCount = 1) to 0,
            space("owner-shared", memberCount = 5) to 1,
            space("editor-shared", role = Role.EDITOR, memberCount = 3) to 0,
            space("viewer-shared", role = Role.VIEWER, memberCount = 3) to 0,
        )
        cases.forEach { (space, expected) ->
            assertEquals(expected, listOf(space).sharedOwnedCount(), space.id)
        }
    }

    @Test
    fun deleteCannotBeConfirmedUntilTheSharedSpaceCountArrives() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.spacesGate = gate
        repository.spacesResult = RestResult.Success(listOf(space("shared")))
        val viewModel = viewModel()

        viewModel.requestDelete()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.loadingSharedSpaces)

        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals(listOf("spaces"), log)
        assertTrue(viewModel.state.value.confirmingDelete)

        gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.loadingSharedSpaces)
        assertEquals(1, viewModel.state.value.sharedSpacesToDelete)

        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals(
            listOf("spaces", "canDeleteNow", "deleteServerAccount", "deleteUser", "signOut", "forgetToken"),
            log
        )
    }

    @Test
    fun deleteWarningWithoutAnAnswerShowsOnlyTheGeneralWarning() = runTest {
        repository.spacesResult = RestResult.Error(DataError.Network)
        val viewModel = viewModel()

        viewModel.requestDelete()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.confirmingDelete)
        assertNull(viewModel.state.value.sharedSpacesToDelete)
        // A failed lookup still lets the deletion go ahead with the general warning
        assertFalse(viewModel.state.value.loadingSharedSpaces)
    }

    @Test
    fun deleteAccountAsksForReauthWhenSessionIsOld() = runTest {
        auth.canDeleteNowResults[0] = AuthResult.NeedsRecentLogin

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow"), log - "spaces")
        assertTrue(viewModel.state.value.needsReauth)
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun deleteAccountDeletesServerDataThenFirebaseUser() = runTest {
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(
            listOf("canDeleteNow", "deleteServerAccount", "deleteUser", "signOut", "forgetToken"),
            log - "spaces"
        )
        assertFalse(viewModel.state.value.confirmingDelete)
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    // Personal data does not outlive the account: a sign-up name still waiting for Firebase goes with it
    @Test
    fun aDeletedAccountLeavesNoPendingName() = runTest {
        pendingName.put("u1", "Ada")
        pendingName.put("u2", "Grace")

        confirmedDeletion()
        advanceUntilIdle()

        val stored = preferences.data.first()
        assertNull(stored[stringPreferencesKey("pendingDisplayName_u1")])
        assertEquals("Grace", stored[stringPreferencesKey("pendingDisplayName_u2")])
    }

    // The account still exists: its name keeps waiting for Firebase
    @Test
    fun aFailedFirebaseDeletionKeepsThePendingName() = runTest {
        auth.deleteUserResults[0] = AuthResult.Failure(AuthFailure.NETWORK)
        pendingName.put("u1", "Ada")

        confirmedDeletion()
        advanceUntilIdle()

        assertEquals("Ada", preferences.data.first()[stringPreferencesKey("pendingDisplayName_u1")])
    }

    @Test
    fun deleteAccountServerFailureKeepsFirebaseUser() = runTest {
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(500))

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow", "deleteServerAccount"), log - "spaces")
        assertEquals(UiError.UNKNOWN, viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)
        assertTrue(auth.state.value is AuthState.SignedIn)
    }

    // DELETE /me counts against the backend's strict limiter: 429 asks the user to wait, not to retry at once
    @Test
    fun aRateLimitedDeletionSaysTryLater() = runTest {
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(429, ErrorCodes.RATE_LIMITED))

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(UiError.RATE_LIMITED, viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)
        assertTrue(auth.state.value is AuthState.SignedIn)
    }

    // A 426 has opened the app-wide forced-update screen already: no error under it, and the Firebase user stays
    @Test
    fun anUpgradeRequiredDeletionShowsNoError() = runTest {
        repository.deleteServerAccountResult =
            RestResult.Error(DataError.Http(426, ErrorCodes.CLIENT_UPGRADE_REQUIRED))

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow", "deleteServerAccount"), log - "spaces")
        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.busy)
        assertTrue(auth.state.value is AuthState.SignedIn)
    }

    @Test
    fun signingInAgainRestartsTheDeletion() = runTest {
        auth.canDeleteNowResults.clear()
        auth.canDeleteNowResults.addAll(listOf(AuthResult.NeedsRecentLogin, AuthResult.Success))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        viewModel.reauthWithEmail("me@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(
            listOf(
                "canDeleteNow", "signInWithEmail:me@example.com", "forgetToken",
                "canDeleteNow", "deleteServerAccount", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
        assertFalse(viewModel.state.value.needsReauth)
    }

    // Firebase may still refuse after canDeleteNow said yes (a race): the server data is gone already, so the
    // second round sends DELETE /me again, which the server answers 204 for an account it no longer has
    @Test
    fun deleteUserRaceAsksForReauthAndTheSecondRoundFinishes() = runTest {
        auth.deleteUserResults.clear()
        auth.deleteUserResults.addAll(listOf(AuthResult.NeedsRecentLogin, AuthResult.Success))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.needsReauth)

        viewModel.reauthWithGoogle()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "canDeleteNow", "deleteServerAccount", "deleteUser",
                "signInWithGoogle", "forgetToken",
                "canDeleteNow", "deleteServerAccount", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
        assertEquals(AuthState.SignedOut, auth.state.value)
    }

    @Test
    fun reauthWithAnotherAccountDeletesNothing() = runTest {
        auth.canDeleteNowResults[0] = AuthResult.NeedsRecentLogin
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        auth.uidAfterSignIn = "someone-else"
        viewModel.reauthWithApple()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow", "signInWithAppleToRevoke"), log - "spaces")
        assertFalse(viewModel.state.value.needsReauth)
        assertFalse(viewModel.state.value.busy)
    }

    // The real AuthService.state follows Firebase's listener and may still show the old user when the sign-in
    // returns: the guard must read Firebase's current uid, not the state
    @Test
    fun reauthWithAnotherAccountDeletesNothingWhileTheStateStillShowsTheOldUser() = runTest {
        auth.canDeleteNowResults[0] = AuthResult.NeedsRecentLogin
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        auth.stateLags = true
        auth.uidAfterSignIn = "someone-else"
        viewModel.reauthWithEmail("other@example.com", "secret1")
        advanceUntilIdle()

        assertEquals(AuthState.SignedIn("u1", "me@example.com"), auth.state.value)
        assertEquals(listOf("canDeleteNow", "signInWithEmail:other@example.com"), log - "spaces")
        assertFalse(viewModel.state.value.busy)
    }

    // Same user, state lagging: deletion goes ahead, and the cached bearer token is dropped before DELETE /me
    @Test
    fun reauthAsTheSameUserForgetsTheCachedTokenBeforeDeletingServerData() = runTest {
        auth.canDeleteNowResults.clear()
        auth.canDeleteNowResults.addAll(listOf(AuthResult.NeedsRecentLogin, AuthResult.Success))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        auth.stateLags = true
        viewModel.reauthWithApple()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "canDeleteNow", "signInWithAppleToRevoke", "forgetToken",
                "canDeleteNow", "deleteServerAccount", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
    }

    @Test
    fun cancelledReauthKeepsAsking() = runTest {
        auth.canDeleteNowResults[0] = AuthResult.NeedsRecentLogin
        auth.google[0] = AuthResult.Cancelled
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        viewModel.reauthWithGoogle()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow", "signInWithGoogle"), log - "spaces")
        assertTrue(viewModel.state.value.needsReauth)
    }

    @Test
    fun unauthorizedServerDeletionSignsOut() = runTest {
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(401))

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("canDeleteNow", "deleteServerAccount", "signOut", "forgetToken"), log - "spaces")
        assertEquals(listOf(SignOutReason.SESSION_ENDED), auth.signOutReasons)
        // The sign-in screen explains the ended session; no error is left on this one
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun anAppleUserSignsInAgainAndIsRevokedBeforeFirebaseDeletion() = runTest {
        auth.usesApple = true

        confirmedDeletion()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "reauthenticateWithAppleToRevoke", "forgetToken", "canDeleteNow", "deleteServerAccount",
                "revokeAppleToken", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
    }

    @Test
    fun closingTheAppleSheetDeletesNothing() = runTest {
        auth.usesApple = true
        auth.appleReauth[0] = AuthResult.Cancelled

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("reauthenticateWithAppleToRevoke"), log - "spaces")
        assertFalse(viewModel.state.value.busy)
        assertTrue(auth.state.value is AuthState.SignedIn)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun anotherAppleAccountDeletesNothing() = runTest {
        auth.usesApple = true
        auth.uidAfterSignIn = "u2"

        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        assertEquals(listOf("reauthenticateWithAppleToRevoke"), log - "spaces")
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun aFailedAppleSignInStillDeletesTheAccount() = runTest {
        auth.usesApple = true
        auth.appleReauth[0] = AuthResult.Failure(AuthFailure.UNAVAILABLE)

        confirmedDeletion()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "reauthenticateWithAppleToRevoke", "canDeleteNow", "deleteServerAccount",
                "revokeAppleToken", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
    }

    @Test
    fun aServerFailureRevokesNothing() = runTest {
        auth.usesApple = true
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(500))

        confirmedDeletion()
        advanceUntilIdle()

        assertEquals(
            listOf("reauthenticateWithAppleToRevoke", "forgetToken", "canDeleteNow", "deleteServerAccount"),
            log - "spaces"
        )
    }

    @Test
    fun aRetrySignsInWithAppleAgain() = runTest {
        auth.usesApple = true
        repository.deleteServerAccountResult = RestResult.Error(DataError.Http(500))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        repository.deleteServerAccountResult = RestResult.Success(Unit)
        viewModel.requestDelete()
        advanceUntilIdle()
        viewModel.confirmDelete()
        advanceUntilIdle()

        assertEquals(2, log.count { it == "reauthenticateWithAppleToRevoke" })
        assertTrue(auth.state.value is AuthState.SignedOut)
    }

    // The first prompt failed, so nothing is held yet: the sign-in that answers NeedsRecentLogin brings the token
    @Test
    fun aFailedAppleReauthThenAReauthRevokesBeforeDeletingFirebaseUser() = runTest {
        auth.usesApple = true
        auth.appleReauth[0] = AuthResult.Failure(AuthFailure.UNAVAILABLE)
        auth.canDeleteNowResults.clear()
        auth.canDeleteNowResults.addAll(listOf(AuthResult.NeedsRecentLogin, AuthResult.Success))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()

        viewModel.reauthWithApple()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "reauthenticateWithAppleToRevoke", "canDeleteNow",
                "signInWithAppleToRevoke", "forgetToken", "canDeleteNow", "deleteServerAccount",
                "revokeAppleToken", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
    }

    // Each round revokes what its own sign-in returned, always before Firebase is asked to delete the user
    @Test
    fun firebaseRefusingDeletionAsksForAFreshSignInAndRevokesAgain() = runTest {
        auth.usesApple = true
        auth.deleteUserResults.clear()
        auth.deleteUserResults.addAll(listOf(AuthResult.NeedsRecentLogin, AuthResult.Success))
        val viewModel = confirmedDeletion()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.needsReauth)

        viewModel.reauthWithApple()
        advanceUntilIdle()

        assertEquals(
            listOf(
                "reauthenticateWithAppleToRevoke", "forgetToken", "canDeleteNow", "deleteServerAccount",
                "revokeAppleToken", "deleteUser",
                "signInWithAppleToRevoke", "forgetToken", "canDeleteNow", "deleteServerAccount",
                "revokeAppleToken", "deleteUser", "signOut", "forgetToken",
            ),
            log - "spaces"
        )
    }

    @Test
    fun signOutSignsOut() = runTest {
        val viewModel = viewModel()

        viewModel.signOut()
        advanceUntilIdle()

        // The cached bearer token goes with the session: the next user's calls never carry it
        assertEquals(listOf("signOut", "forgetToken"), log)
        assertEquals(listOf(SignOutReason.USER), auth.signOutReasons)
    }
}
