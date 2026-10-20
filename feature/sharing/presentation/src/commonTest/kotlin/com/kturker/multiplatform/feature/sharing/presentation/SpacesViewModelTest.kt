package com.kturker.multiplatform.feature.sharing.presentation

import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.feature.sharing.contract.MembersDestination
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError
import com.kturker.multiplatform.feature.sharing.presentation.ui.spaces.SpacesDialog
import com.kturker.multiplatform.feature.sharing.presentation.ui.spaces.SpacesViewModel
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SpacesViewModelTest {

    private val log = mutableListOf<String>()
    private val auth = FakeAuthService(log)
    private val repository = FakeSpacesRepository(log)
    private val selectedSpace = FakeSelectedSpace(log)
    private val navigation = RecordingNavigationManager()

    private val personal = space("personal", personal = true)
    private val owned = space("owned", memberCount = 3)
    private val joined = space("joined", role = Role.EDITOR, memberCount = 2)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        repository.listResult = RestResult.Success(listOf(owned, joined, personal))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SpacesViewModel(
        repository = repository,
        auth = auth,
        tokens = FakeSessionTokens(log),
        selectedSpace = selectedSpace,
        navigationManager = navigation
    )

    @Test
    fun thePersonalSpaceComesFirst() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(personal, owned, joined), viewModel.state.value.spaces)
    }

    @Test
    fun theSelectionFollowsSelectedSpace() = runTest {
        selectedSpace.current.value = owned
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals("owned", viewModel.state.value.selectedId)
    }

    // The owner renames or deletes a space (not the personal one); a member leaves it
    @Test
    fun actionsFollowTheRole() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        val state = viewModel.state.value

        assertTrue(state.canRename(owned) && state.canDelete(owned) && !state.canLeave(owned))
        assertTrue(!state.canRename(joined) && !state.canDelete(joined) && state.canLeave(joined))
        assertFalse(state.canDelete(personal))
        assertFalse(state.canLeave(personal))
    }

    // An invite accepted over this screen selects the joined space, which the loaded list does not have yet
    @Test
    fun aSelectedSpaceMissingFromTheListReloadsIt() = runTest {
        repository.listResult = RestResult.Success(listOf(owned, personal))
        val viewModel = viewModel()
        advanceUntilIdle()
        repository.listResult = RestResult.Success(listOf(owned, joined, personal))
        log.clear()

        selectedSpace.current.value = joined
        advanceUntilIdle()

        assertEquals(listOf("list"), log)
        assertEquals(listOf(personal, owned, joined), viewModel.state.value.spaces)
        assertEquals("joined", viewModel.state.value.selectedId)
    }

    @Test
    fun aSelectedSpaceAlreadyInTheListDoesNotReload() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        log.clear()

        selectedSpace.current.value = owned
        advanceUntilIdle()

        assertEquals(emptyList(), log)
        assertEquals("owned", viewModel.state.value.selectedId)
    }

    @Test
    fun selectingASpaceSelectsIt() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.select("joined")
        advanceUntilIdle()

        assertTrue("select:joined" in log)
    }

    @Test
    fun openingASpaceShowsItsMembers() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openMembers("owned")

        assertEquals(
            listOf<NavigationCommand>(NavigationCommand.NavigateTo(MembersDestination("owned"))),
            navigation.commands
        )
    }

    @Test
    fun creatingASpaceTrimsTheNameAndReloads() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startCreate()
        assertEquals(SpacesDialog.Create, viewModel.state.value.dialog)
        viewModel.submitName("  Home  ")
        advanceUntilIdle()

        assertTrue("create:Home" in log)
        assertEquals(2, log.count { it == "list" })
        assertNull(viewModel.state.value.dialog)
    }

    @Test
    fun aBlankNameIsNotSent() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startCreate()
        viewModel.submitName("   ")
        advanceUntilIdle()

        assertTrue(log.none { it.startsWith("create") })
    }

    @Test
    fun renamingRefreshesTheSelection() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.startRename(owned)
        viewModel.submitName("Our home")
        advanceUntilIdle()

        assertTrue("rename:owned:Our home" in log)
        assertTrue("refresh" in log)
    }

    @Test
    fun deletingAsksFirstThenDeletesAndRefreshesTheSelection() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestDelete(owned)
        assertEquals(SpacesDialog.ConfirmDelete(owned), viewModel.state.value.dialog)
        assertTrue(log.none { it.startsWith("delete") })
        viewModel.confirm()
        advanceUntilIdle()

        assertTrue("delete:owned" in log)
        assertTrue("refresh" in log)
    }

    // POST /spaces/{id}/leave: the server knows who is asking, the app sends no user id
    @Test
    fun leavingCallsLeave() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestLeave(joined)
        assertEquals(SpacesDialog.ConfirmLeave(joined), viewModel.state.value.dialog)
        viewModel.confirm()
        advanceUntilIdle()

        assertTrue("leave:joined" in log)
        assertTrue(log.none { it.startsWith("remove") })
        assertTrue("refresh" in log)
    }

    // Deleted by its owner, or the user was removed, before the tap
    @Test
    fun notFoundRefreshesAndSaysThereIsNoAccess() = runTest {
        repository.deleteResult = RestResult.Error(DataError.Http(404))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.requestDelete(owned)
        viewModel.confirm()
        advanceUntilIdle()

        assertTrue("refresh" in log)
        assertEquals(SharingError.NO_ACCESS, viewModel.state.value.error)
    }

    @Test
    fun unauthorizedSignsOutAndDropsTheToken() = runTest {
        repository.listResult = RestResult.Error(DataError.Http(401))

        viewModel()
        advanceUntilIdle()

        assertEquals(listOf("list", "signOut:${SignOutReason.SESSION_ENDED}", "forgetToken"), log)
    }

    // 429 RATE_LIMITED: the backend's limiter, not a broken request
    @Test
    fun rateLimitedSaysTryLater() = runTest {
        repository.listResult = RestResult.Error(DataError.Http(429, ErrorCodes.RATE_LIMITED))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(SharingError.RATE_LIMITED, viewModel.state.value.error)
        assertTrue(log.none { it.startsWith("signOut") })
    }

    // 426 opens the app-wide forced-update screen (the client calls it); this screen adds no message of its own
    @Test
    fun upgradeRequiredShowsNoMessage() = runTest {
        repository.listResult = RestResult.Error(DataError.Http(426, ErrorCodes.CLIENT_UPGRADE_REQUIRED))

        val viewModel = viewModel()
        advanceUntilIdle()

        assertNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun noConnectionSaysSo() = runTest {
        repository.listResult = RestResult.Error(DataError.Network)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(SharingError.NETWORK, viewModel.state.value.error)
    }
}
