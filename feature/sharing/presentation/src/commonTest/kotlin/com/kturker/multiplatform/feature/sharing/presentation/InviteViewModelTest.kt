package com.kturker.multiplatform.feature.sharing.presentation

import androidx.lifecycle.SavedStateHandle
import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.InvitePreview
import com.kturker.multiplatform.core.api.InviteState
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.feature.account.contract.LoginDestination
import com.kturker.multiplatform.feature.sharing.contract.InviteDestination
import com.kturker.multiplatform.feature.sharing.presentation.data.DataStorePendingInviteStore
import com.kturker.multiplatform.feature.sharing.presentation.ui.invite.InviteMessage
import com.kturker.multiplatform.feature.sharing.presentation.ui.invite.InviteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InviteViewModelTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log)
    private val repository = FakeSpacesRepository(log)
    private val selectedSpace = FakeSelectedSpace(log)
    private val pending = DataStorePendingInviteStore(InMemoryPreferences())
    private val navigation = RecordingNavigationManager()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(token: String = "tok") = InviteViewModel(
        savedStateHandle = SavedStateHandle(mapOf(InviteDestination::token.name to token)),
        repository = repository,
        auth = auth,
        tokens = FakeSessionTokens(log),
        pendingInvites = pending,
        selectedSpace = selectedSpace,
        navigationManager = navigation
    )

    private fun preview(state: InviteState) =
        RestResult.Success(
            InvitePreview(spaceName = "Home", inviterName = "Ada", role = Role.EDITOR, expiresAt = 0, state = state)
        )

    @Test
    fun aValidInviteShowsItsPreview() = runTest {
        repository.previewResult = preview(InviteState.VALID)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("Home", viewModel.state.value.preview?.spaceName)
        assertTrue(viewModel.state.value.canAccept)
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun acceptingSelectsTheSpaceAndGoesBack() = runTest {
        repository.previewResult = preview(InviteState.VALID)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.accept()
        advanceUntilIdle()

        assertEquals(listOf("preview:tok", "accept:tok", "select:joined"), log)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigation.commands)
    }

    @Test
    fun decliningGoesBackWithoutAccepting() = runTest {
        repository.previewResult = preview(InviteState.VALID)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.close()
        advanceUntilIdle()

        assertEquals(listOf("preview:tok"), log)
        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigation.commands)
    }

    @Test
    fun aUsedInviteExplainsItself() = runTest {
        repository.previewResult = preview(InviteState.USED)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.USED, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
    }

    @Test
    fun anExpiredInviteExplainsItself() = runTest {
        repository.previewResult = preview(InviteState.EXPIRED)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.EXPIRED, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
    }

    // A state a newer server sends and this app does not know: no Accept, the generic message
    @Test
    fun anInviteInAStateTheAppDoesNotKnowCannotBeAccepted() = runTest {
        repository.previewResult = preview(InviteState.UNKNOWN)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.UNKNOWN, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
    }

    @Test
    fun anUnknownInviteIsNotFound() = runTest {
        repository.previewResult = RestResult.Error(DataError.Http(404))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.NOT_FOUND, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
    }

    // Someone else accepted it, or it ran out, between the preview and the tap
    @Test
    fun acceptingAnInviteThatWasUsedOrExpiredMeanwhileExplainsItself() = runTest {
        repository.previewResult = preview(InviteState.VALID)
        repository.acceptResult = RestResult.Error(DataError.Http(409, ErrorCodes.INVITE_USED))
        val usedMeanwhile = viewModel()
        advanceUntilIdle()

        usedMeanwhile.accept()
        advanceUntilIdle()
        assertEquals(InviteMessage.USED, usedMeanwhile.state.value.message)
        assertFalse(usedMeanwhile.state.value.canAccept)

        repository.acceptResult = RestResult.Error(DataError.Http(410, ErrorCodes.INVITE_EXPIRED))
        val expiredMeanwhile = viewModel()
        advanceUntilIdle()
        expiredMeanwhile.accept()
        advanceUntilIdle()
        assertEquals(InviteMessage.EXPIRED, expiredMeanwhile.state.value.message)

        assertFalse("select:joined" in log)
        assertTrue(navigation.commands.isEmpty())
    }

    // The owner revoked the link before it was opened; asking again changes nothing
    @Test
    fun revokedPreviewShowsRevoked() = runTest {
        repository.previewResult = preview(InviteState.REVOKED)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.REVOKED, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
        assertFalse(viewModel.state.value.canRetry)
    }

    // A valid preview, then the accept tap meets the backend's answer
    private fun TestScope.acceptAnswering(error: DataError): InviteViewModel {
        repository.previewResult = preview(InviteState.VALID)
        repository.acceptResult = RestResult.Error(error)
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.accept()
        advanceUntilIdle()
        return viewModel
    }

    // The owner revoked the link between the preview and the tap
    @Test
    fun acceptRevokedShowsRevoked() = runTest {
        val viewModel = acceptAnswering(DataError.Http(410, ErrorCodes.INVITE_REVOKED))

        assertEquals(InviteMessage.REVOKED, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
        assertFalse(viewModel.state.value.canRetry)
        assertFalse("select:joined" in log)
    }

    @Test
    fun acceptExpiredStillShowsExpired() = runTest {
        val viewModel = acceptAnswering(DataError.Http(410, ErrorCodes.INVITE_EXPIRED))

        assertEquals(InviteMessage.EXPIRED, viewModel.state.value.message)
    }

    // The space reached its member limit; the 409 is not "already used"
    @Test
    fun acceptIntoFullSpaceShowsSpaceFull() = runTest {
        val viewModel = acceptAnswering(DataError.Http(409, ErrorCodes.QUOTA_EXCEEDED))

        assertEquals(InviteMessage.SPACE_FULL, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canAccept)
        assertFalse(viewModel.state.value.canRetry)
        assertFalse("select:joined" in log)
        assertTrue(navigation.commands.isEmpty())
    }

    @Test
    fun acceptUsedStillShowsUsed() = runTest {
        val viewModel = acceptAnswering(DataError.Http(409, ErrorCodes.INVITE_USED))

        assertEquals(InviteMessage.USED, viewModel.state.value.message)
    }

    // 426 opens the app-wide forced-update screen; the invite adds no message under it
    @Test
    fun upgradeRequiredShowsNoMessage() = runTest {
        repository.previewResult = RestResult.Error(DataError.Http(426, ErrorCodes.CLIENT_UPGRADE_REQUIRED))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.loading)
        assertFalse(viewModel.state.value.canAccept)
        assertTrue(log.none { it.startsWith("signOut") })
    }

    // Every invite call counts against the backend's strict limiter
    @Test
    fun rateLimitedSaysTryLaterAndCanBeRetried() = runTest {
        repository.previewResult = RestResult.Error(DataError.Http(429, ErrorCodes.RATE_LIMITED))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(InviteMessage.RATE_LIMITED, viewModel.state.value.message)
        assertTrue(viewModel.state.value.canRetry)
    }

    @Test
    fun noConnectionCanBeRetried() = runTest {
        repository.previewResult = RestResult.Error(DataError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(InviteMessage.NETWORK, viewModel.state.value.message)

        repository.previewResult = preview(InviteState.VALID)
        viewModel.retry()
        advanceUntilIdle()

        assertNull(viewModel.state.value.message)
        assertTrue(viewModel.state.value.canAccept)
    }

    // Signed out, the link waits for the sign-in; after it the app shell takes it and opens the
    // preview, which now loads
    @Test
    fun aPendingInviteSurvivesTheSignIn() = runTest {
        auth.state.value = AuthState.SignedOut
        repository.previewResult = preview(InviteState.VALID)

        viewModel(token = "abc")
        advanceUntilIdle()

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.NavigateTo(LoginDestination, clearBackStack = true)),
            navigation.commands
        )
        assertTrue(log.none { it.startsWith("preview") }, "nothing is asked of the server while signed out")

        // The user signs in; the app shell (MainScreen) takes the waiting invite and opens it
        auth.state.value = AuthState.SignedIn("me", "me@example.com")
        val token = assertNotNull(pending.take())
        assertEquals("abc", token)
        val reopened = viewModel(token = InviteDestination(token).token)
        advanceUntilIdle()

        assertEquals("Home", reopened.state.value.preview?.spaceName)
        assertNull(pending.take(), "the invite is taken once")
    }

    // A cold start from the link: Firebase has not reported the restored session yet
    @Test
    fun theInviteWaitsForTheRestoredSession() = runTest {
        auth.state.value = AuthState.Loading
        repository.previewResult = preview(InviteState.VALID)
        val viewModel = viewModel()
        advanceUntilIdle()
        assertTrue(log.isEmpty())

        auth.state.value = AuthState.SignedIn("me", "me@example.com")
        advanceUntilIdle()

        assertEquals("Home", viewModel.state.value.preview?.spaceName)
        assertTrue(navigation.commands.isEmpty())
    }

    // The session ended (401 after the token refresh): sign out by the account rule, and keep the invite for
    // the sign-in that follows
    @Test
    fun anUnauthorizedAnswerSignsOutAndKeepsTheInvite() = runTest {
        repository.previewResult = RestResult.Error(DataError.Http(401))

        viewModel()
        advanceUntilIdle()

        assertEquals(listOf("preview:tok", "signOut:${SignOutReason.SESSION_ENDED}", "forgetToken"), log)
        assertEquals("tok", pending.take())
    }
}
