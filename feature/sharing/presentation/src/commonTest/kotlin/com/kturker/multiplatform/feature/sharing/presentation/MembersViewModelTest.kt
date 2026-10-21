package com.kturker.multiplatform.feature.sharing.presentation

import androidx.lifecycle.SavedStateHandle
import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.PendingInvite
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.DEEP_LINK_SCHEME
import com.kturker.multiplatform.feature.sharing.contract.MembersDestination
import com.kturker.multiplatform.feature.sharing.presentation.share.InviteText
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError
import com.kturker.multiplatform.feature.sharing.presentation.ui.members.MembersViewModel
import com.kturker.multiplatform.feature.sharing.presentation.ui.members.daysLeft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MembersViewModelTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log)
    private val repository = FakeSpacesRepository(log)
    private val selectedSpace = FakeSelectedSpace(log)
    private val sharer = RecordingSharer()

    private val me = member("me", role = Role.OWNER)
    private val bob = member("bob", role = Role.EDITOR)
    private val editorInvite = PendingInvite(id = "i1", role = Role.EDITOR, createdAt = 1, expiresAt = 2)
    private val viewerInvite = PendingInvite(id = "i2", role = Role.VIEWER, createdAt = 1, expiresAt = 2)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository.listResult = RestResult.Success(listOf(space("home", role = Role.OWNER, memberCount = 2)))
        repository.membersResult = RestResult.Success(listOf(me, bob))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MembersViewModel(
        savedStateHandle = SavedStateHandle(mapOf(MembersDestination::spaceId.name to "home")),
        repository = repository,
        auth = auth,
        tokens = FakeSessionTokens(log),
        selectedSpace = selectedSpace,
        sharer = sharer,
        inviteText = InviteText { url -> "Join my list in Test: $url" },
        navigationManager = RecordingNavigationManager()
    )

    @Test
    fun listsTheMembersOfTheSpace() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(me, bob), viewModel.state.value.members)
        assertEquals("home", viewModel.state.value.space?.id)
    }

    // "(you)" follows GET /me: the backend's own id for the user, which is not the sign-in provider's uid
    @Test
    fun youIsTheBackendUserNotTheFirebaseUid() = runTest {
        val backendMe = member("backend-me", role = Role.OWNER)
        // Someone whose backend id happens to equal the Firebase uid (FakeAuthService's "me")
        val firebaseNamesake = member("me")
        repository.membersResult = RestResult.Success(listOf(backendMe, firebaseNamesake))

        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertTrue("me" in log)
        assertEquals("backend-me", state.myUserId)
        assertTrue(state.isYou(backendMe))
        assertFalse(state.isYou(firebaseNamesake))
    }

    // Without an answer from GET /me the list still shows; only the "(you)" mark is missing
    @Test
    fun aFailedMeStillListsTheMembers() = runTest {
        repository.meResult = RestResult.Error(DataError.Network)

        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertEquals(listOf(me, bob), state.members)
        assertEquals(null, state.myUserId)
        assertEquals(null, state.error)
        assertFalse(state.members.any { state.isYou(it) })
    }

    @Test
    fun theOwnerManagesOthersAndShares() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertTrue(state.canShare)
        assertTrue(state.canManage(bob))
        // Not themselves: the owner's role never changes, and the owner cannot be removed
        assertFalse(state.canManage(me))
    }

    @Test
    fun aMemberWhoIsNotTheOwnerSeesNoRoleChangeRemoveOrShare() = runTest {
        repository.listResult = RestResult.Success(listOf(space("home", role = Role.EDITOR, memberCount = 2)))
        repository.membersResult = RestResult.Success(listOf(member("owner", role = Role.OWNER), member("me")))
        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertFalse(state.canShare)
        assertTrue(state.members.none { state.canManage(it) })
    }

    @Test
    fun shareCreatesAnInviteAndHandsItsLinkToTheShareSheet() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startShare()
        assertTrue(viewModel.state.value.choosingInviteRole)
        viewModel.share(Role.VIEWER)
        advanceUntilIdle()

        assertTrue("invite:home:VIEWER" in log)
        val text = sharer.shared.single()
        assertTrue(text.contains("$DEEP_LINK_SCHEME://invite/tok"), text)
        assertFalse(viewModel.state.value.choosingInviteRole)
    }

    @Test
    fun theOwnerChangesARoleAndTheListReloads() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.changeRole(bob, Role.VIEWER)
        advanceUntilIdle()

        assertTrue("setRole:home:bob:VIEWER" in log)
        assertEquals(2, log.count { it == "members:home" })
    }

    @Test
    fun theOwnerRemovesAMemberAfterConfirming() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestRemove(bob)
        assertEquals(bob, viewModel.state.value.removing)
        viewModel.confirmRemove()
        advanceUntilIdle()

        assertTrue("remove:home:bob" in log)
        assertEquals(null, viewModel.state.value.removing)
    }

    // The space was deleted, or the user was removed from it, meanwhile
    @Test
    fun notFoundRefreshesTheSelectionAndSaysThereIsNoAccess() = runTest {
        repository.membersResult = RestResult.Error(DataError.Http(404))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue("refresh" in log)
        assertEquals(SharingError.NO_ACCESS, viewModel.state.value.error)
    }

    // The target member left (or was removed elsewhere) meanwhile: the server answers 404 for them, not for the
    // space. The owner keeps access; the list reloads without the departed member.
    @Test
    fun aMemberGoneMeanwhileReloadsTheListInsteadOfLosingAccess() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.setRoleResult = RestResult.Error(DataError.Http(404))
        repository.removeResult = RestResult.Error(DataError.Http(404))
        repository.membersResult = RestResult.Success(listOf(me))

        viewModel.changeRole(bob, Role.VIEWER)
        advanceUntilIdle()

        assertEquals(listOf(me), viewModel.state.value.members)
        assertEquals(null, viewModel.state.value.error)
        assertFalse("refresh" in log)

        repository.membersResult = RestResult.Success(listOf(me, bob))
        viewModel.requestRemove(bob)
        viewModel.confirmRemove()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.error)
        assertFalse("refresh" in log)
        assertEquals(3, log.count { it == "members:home" })
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun aSpaceNoLongerInTheListHasNoAccess() = runTest {
        repository.listResult = RestResult.Success(listOf(space("personal", personal = true)))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue("refresh" in log)
        assertEquals(SharingError.NO_ACCESS, viewModel.state.value.error)
    }

    @Test
    fun unauthorizedSignsOut() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.inviteResult = RestResult.Error(DataError.Http(401))

        viewModel.share(Role.EDITOR)
        advanceUntilIdle()

        assertEquals(listOf("signOut:${SignOutReason.SESSION_ENDED}", "forgetToken"), log.takeLast(2))
        assertTrue(sharer.shared.isEmpty())
    }

    @Test
    fun ownerSeesPendingInvites() = runTest {
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite, viewerInvite))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue("pendingInvites:home" in log)
        assertEquals(2, viewModel.state.value.invites.size)
    }

    // The members already loaded stay on screen; the failed invite list only shows its error
    @Test
    fun aFailedInviteListStillListsTheMembers() = runTest {
        repository.pendingInvitesResult = RestResult.Error(DataError.Network)

        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertTrue("pendingInvites:home" in log)
        assertEquals(listOf(me, bob), state.members)
        assertTrue(state.invites.isEmpty())
        assertEquals(SharingError.NETWORK, state.error)
        assertFalse(state.loading)
    }

    // Only the owner sees the invites (the server answers anyone else 404): nobody else asks for them
    @Test
    fun nonOwnerDoesNotAskForInvites() = runTest {
        repository.listResult = RestResult.Success(listOf(space("home", role = Role.EDITOR, memberCount = 2)))
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite, viewerInvite))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(0, log.count { it.startsWith("pendingInvites") })
        assertTrue(viewModel.state.value.invites.isEmpty())
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun shareOverInviteQuotaShowsInviteLimit() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.inviteResult = RestResult.Error(DataError.Http(409, ErrorCodes.QUOTA_EXCEEDED))

        viewModel.share(Role.EDITOR)
        advanceUntilIdle()

        assertEquals(SharingError.INVITE_LIMIT, viewModel.state.value.error)
        assertTrue(sharer.shared.isEmpty())
        assertFalse(viewModel.state.value.busy)
    }

    // The new link joins the pending list
    @Test
    fun shareReloadsInvites() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite))

        viewModel.share(Role.EDITOR)
        advanceUntilIdle()

        assertEquals(2, log.count { it == "pendingInvites:home" })
        assertEquals(listOf(editorInvite), viewModel.state.value.invites)
    }

    @Test
    fun confirmRevokeRevokesAndReloads() = runTest {
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite, viewerInvite))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestRevoke(editorInvite)
        assertEquals(editorInvite, viewModel.state.value.revoking)
        repository.pendingInvitesResult = RestResult.Success(listOf(viewerInvite))
        viewModel.confirmRevoke()
        advanceUntilIdle()

        assertTrue("revokeInvite:home:i1" in log)
        assertEquals(listOf(viewerInvite), viewModel.state.value.invites)
        assertEquals(null, viewModel.state.value.revoking)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun dismissRevokeKeepsTheInvite() = runTest {
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestRevoke(editorInvite)
        viewModel.dismissRevoke()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.revoking)
        assertTrue(log.none { it.startsWith("revokeInvite") })
    }

    // Someone joined with the link meanwhile: it is no longer pending, so the list reloads without an error
    @Test
    fun revokeOfUsedInviteReloadsWithoutError() = runTest {
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite))
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.revokeInviteResult = RestResult.Error(DataError.Http(409, ErrorCodes.INVITE_USED))
        repository.pendingInvitesResult = RestResult.Success(emptyList())

        viewModel.requestRevoke(editorInvite)
        viewModel.confirmRevoke()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.error)
        assertTrue(viewModel.state.value.invites.isEmpty())
        assertEquals(2, log.count { it == "pendingInvites:home" })
        assertFalse(viewModel.state.value.busy)
    }

    // The invite is gone (revoked elsewhere, or run out and cleared): 404 reloads too, the space stays
    @Test
    fun revokeOfAnInviteGoneMeanwhileReloadsWithoutError() = runTest {
        repository.pendingInvitesResult = RestResult.Success(listOf(editorInvite))
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.revokeInviteResult = RestResult.Error(DataError.Http(404, ErrorCodes.NOT_FOUND))
        repository.pendingInvitesResult = RestResult.Success(emptyList())

        viewModel.requestRevoke(editorInvite)
        viewModel.confirmRevoke()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.error)
        assertTrue(viewModel.state.value.invites.isEmpty())
        assertFalse("refresh" in log)
    }

    // Any part of a day left counts as a whole day; an invite never shows zero days
    @Test
    fun daysLeftRoundsUpToWholeDays() {
        val now = 1_000_000L
        val day = 86_400_000L

        assertEquals(1, daysLeft(now + 1, now))
        assertEquals(7, daysLeft(now + day * 7, now))
        assertEquals(7, daysLeft(now + day * 6 + 1, now))
        assertEquals(1, daysLeft(now, now))
    }
}
