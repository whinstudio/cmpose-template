package com.kturker.multiplatform

import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.feature.account.contract.LoginDestination
import com.kturker.multiplatform.feature.sharing.contract.InviteDestination
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.PendingInviteStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainScreenGateTest {

    @Serializable
    private data object Home : NavigationCommand.Destination

    private val log = mutableListOf<String>()
    private val tokens = SessionTokens { log += "forget" }
    private val selectedSpace = RecordingSelectedSpace(log)
    private val pendingInvites = MemoryPendingInvites(log)

    private fun TestScope.gate() = SessionGate(
        home = Home,
        tokens = tokens,
        selectedSpace = selectedSpace,
        pendingInvites = pendingInvites,
        scope = backgroundScope
    )

    // The pure part: (AuthState, pending invite) -> where the shell goes

    @Test
    fun loadingWaitsOnAnEmptyScreen() {
        assertEquals(GateTarget.Wait, gateTarget(AuthState.Loading, pendingInvite = null))
        assertEquals(GateTarget.Wait, gateTarget(AuthState.Loading, pendingInvite = "abc"))
    }

    @Test
    fun signedOutGoesToTheSignInAndTheInviteWaits() {
        assertEquals(GateTarget.SignIn, gateTarget(AuthState.SignedOut, pendingInvite = null))
        assertEquals(GateTarget.SignIn, gateTarget(AuthState.SignedOut, pendingInvite = "abc"))
    }

    // An app without a Firebase config opens the sign-in screen, which says so
    @Test
    fun withoutFirebaseTheSignInOpens() {
        assertEquals(GateTarget.SignIn, gateTarget(AuthState.Unavailable, pendingInvite = null))
    }

    @Test
    fun signedInGoesHomeOrToTheWaitingInvite() {
        val signedIn = AuthState.SignedIn(uid = "u1", email = null)

        assertEquals(GateTarget.Home, gateTarget(signedIn, pendingInvite = null))
        assertEquals(GateTarget.Invite("abc"), gateTarget(signedIn, pendingInvite = "abc"))
    }

    // The session gate: first screen, user changes, token and selection

    @Test
    fun loadingDoesNothing() = runTest {
        assertNull(gate().onState(AuthState.Loading))
        assertEquals(emptyList(), log)
    }

    @Test
    fun aRestoredSessionStartsHomeAndLoadsTheSpace() = runTest {
        val gate = gate()

        val step = gate.onState(signedIn("u1"))
        runCurrent()

        assertEquals(GateStep(start = Home, commands = emptyList()), step)
        assertEquals(listOf("take", "refresh"), log)
    }

    @Test
    fun aSignedOutStartOpensTheSignInAndForgetsTheLastSelection() = runTest {
        val step = gate().onState(AuthState.SignedOut)
        runCurrent()

        assertEquals(GateStep(start = LoginDestination, commands = emptyList()), step)
        // The invite waits for the sign-in; the selection of whoever used the app before is dropped
        assertEquals(listOf("clear"), log)
    }

    @Test
    fun withoutFirebaseTheAppStartsOnTheSignIn() = runTest {
        val step = gate().onState(AuthState.Unavailable)

        assertEquals(GateStep(start = LoginDestination, commands = emptyList()), step)
    }

    @Test
    fun signingInLeavesTheSignInForHome() = runTest {
        val gate = gate()
        gate.onState(AuthState.SignedOut)
        log.clear()

        val step = gate.onState(signedIn("u1"))
        runCurrent()

        assertEquals(GateStep(start = null, commands = listOf(home(clear = true))), step)
        assertEquals(listOf("forget", "clear", "take", "refresh"), log)
    }

    // An invite that waited for the sign-in opens on top of home, so its Accept and Close go back there
    @Test
    fun anInviteWaitingForTheSignInOpensOnTopOfHome() = runTest {
        val gate = gate()
        gate.onState(AuthState.SignedOut)
        pendingInvites.put("abc")

        val step = gate.onState(signedIn("u1"))

        assertEquals(
            GateStep(start = null, commands = listOf(home(clear = true), NavigationCommand.NavigateTo(InviteDestination("abc")))),
            step
        )
        // Taken once: a later change of user does not open it again
        assertNull(pendingInvites.token)
    }

    @Test
    fun aRestoredSessionWithAWaitingInviteStartsHomeThenOpensIt() = runTest {
        pendingInvites.put("abc")

        val step = gate().onState(signedIn("u1"))

        assertEquals(GateStep(start = Home, commands = listOf(NavigationCommand.NavigateTo(InviteDestination("abc")))), step)
    }

    @Test
    fun signingOutForgetsTheTokenAndTheSelectionAndOpensTheSignIn() = runTest {
        val gate = gate()
        gate.onState(signedIn("u1"))
        runCurrent()
        log.clear()

        val step = gate.onState(AuthState.SignedOut)
        runCurrent()

        assertEquals(GateStep(start = null, commands = listOf(signIn())), step)
        // No take (the invite of a signed-out user waits) and no refresh (nobody to load spaces for)
        assertEquals(listOf("forget", "clear"), log)
    }

    @Test
    fun anotherUserGetsTheirOwnTokenAndSpace() = runTest {
        val gate = gate()
        gate.onState(signedIn("u1"))
        runCurrent()
        log.clear()

        val step = gate.onState(signedIn("u2"))
        runCurrent()

        assertEquals(GateStep(start = null, commands = listOf(home(clear = true))), step)
        assertEquals(listOf("forget", "clear", "take", "refresh"), log)
    }

    // A fresh sign-in as the same user (before deleting the account) changes nothing
    @Test
    fun theSameUserAgainChangesNothing() = runTest {
        val gate = gate()
        gate.onState(signedIn("u1"))
        runCurrent()
        log.clear()

        assertNull(gate.onState(signedIn("u1")))
        assertNull(gate.onState(AuthState.Loading))
        assertEquals(emptyList(), log)
    }

    // A refresh still running for the previous user is cancelled before the selection is cleared
    @Test
    fun aRefreshForThePreviousUserIsCancelled() = runTest {
        selectedSpace.suspendRefresh = true
        val gate = gate()
        gate.onState(signedIn("u1"))
        runCurrent()

        gate.onState(AuthState.SignedOut)
        runCurrent()

        assertEquals(1, selectedSpace.cancelledRefreshes)
    }

    private fun signedIn(uid: String) = AuthState.SignedIn(uid = uid, email = null)

    private fun home(clear: Boolean) = NavigationCommand.NavigateTo(Home, clearBackStack = clear)

    private fun signIn() = NavigationCommand.NavigateTo(LoginDestination, clearBackStack = true)
}

private class RecordingSelectedSpace(private val log: MutableList<String>) : SelectedSpace {
    override val current = MutableStateFlow<Space?>(null)
    var suspendRefresh = false
    var cancelledRefreshes = 0

    override suspend fun select(spaceId: String) {
        log += "select:$spaceId"
    }

    override suspend fun refresh() {
        log += "refresh"
        if (suspendRefresh) {
            try {
                kotlinx.coroutines.awaitCancellation()
            } finally {
                cancelledRefreshes++
            }
        }
        current.value = Space(id = "personal", name = "Personal", role = Role.OWNER, personal = true, memberCount = 1)
    }

    override suspend fun clear() {
        log += "clear"
        current.value = null
    }
}

private class MemoryPendingInvites(private val log: MutableList<String>) : PendingInviteStore {
    var token: String? = null

    override suspend fun put(token: String) {
        this.token = token
    }

    override suspend fun take(): String? {
        log += "take"
        return token.also { token = null }
    }
}
