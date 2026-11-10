package com.kturker.multiplatform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.feature.account.contract.LoginDestination
import com.kturker.multiplatform.feature.sharing.contract.InviteDestination
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.PendingInviteTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Where the app shell goes for a session state. */
internal sealed interface GateTarget {
    /** Firebase has not reported the restored session yet: an empty screen. */
    data object Wait : GateTarget

    /** Nobody is signed in, or the app has no Firebase config (the sign-in screen says so). */
    data object SignIn : GateTarget

    /** The app's own first screen. */
    data object Home : GateTarget

    /** An invite link opened while signed out: its preview, on top of the first screen. */
    data class Invite(val token: String) : GateTarget
}

internal fun gateTarget(
    state: AuthState,
    pendingInvite: String?
): GateTarget = when (state) {
    AuthState.Loading -> GateTarget.Wait
    AuthState.SignedOut, AuthState.Unavailable -> GateTarget.SignIn
    is AuthState.SignedIn -> pendingInvite?.let(GateTarget::Invite) ?: GateTarget.Home
}

/**
 * What the shell does for a session change: [start] is the NavHost's first screen (set once, when the restored
 * session is known), [commands] go to the NavigationManager after it.
 */
internal data class GateStep(val start: NavigationCommand.Destination?, val commands: List<NavigationCommand>)

/**
 * The sign-in gate, keyed by uid. The first settled state starts the app on [home] or on the sign-in; every later
 * change of user (sign-in, sign-out, another account, a 401 that ended the session) drops the cached bearer token
 * and the previous user's selected space, then opens [home] or the sign-in on a cleared back stack. A signed-in
 * user's spaces load on [scope], and again on each of [reconnections] while no space is shown (an offline start);
 * an invite that waited for the sign-in opens on top of [home], so its Accept and Close go back there. A fresh
 * sign-in as the same user (before deleting the account) changes nothing.
 */
internal class SessionGate(
    private val home: NavigationCommand.Destination,
    private val tokens: SessionTokens,
    private val selectedSpace: SelectedSpace,
    private val pendingInvites: PendingInviteTokenStore,
    private val reconnections: Flow<Unit>,
    private val scope: CoroutineScope
) {
    private var started = false
    private var uid: String? = null
    private var refresh: Job? = null

    /** Null: nothing to do (still loading, or the same user). */
    suspend fun onState(state: AuthState): GateStep? {
        if (state == AuthState.Loading) return null
        val newUid = (state as? AuthState.SignedIn)?.uid
        val first = !started
        if (!first && newUid == uid) return null
        started = true
        uid = newUid

        if (!first) {
            tokens.forget()
            // The previous user's refresh (or a reconnection's) would show their space to the next one
            refresh?.cancelAndJoin()
        }
        // A signed-out start clears too: the last user's choice must not reach whoever signs in next
        if (!first || newUid == null) selectedSpace.clear()
        val target = gateTarget(state, pendingInvite = if (newUid != null) pendingInvites.take() else null)
        if (newUid != null) refresh = scope.launch { loadSpaces() }
        return step(target, first)
    }

    // The user's job: cancelling it stops the refresh and the reconnection collector together
    private suspend fun loadSpaces() = coroutineScope {
        var loading = launch { selectedSpace.refresh() }
        // Started offline: nothing to show until the connection comes back
        reconnections.collect {
            if (selectedSpace.current.value == null && !loading.isActive) loading = launch { selectedSpace.refresh() }
        }
    }

    private fun step(
        target: GateTarget,
        first: Boolean
    ): GateStep {
        val base = if (target == GateTarget.SignIn) LoginDestination else home
        val invite = (target as? GateTarget.Invite)
            ?.let { listOf(NavigationCommand.NavigateTo(InviteDestination(it.token))) }
            .orEmpty()
        return if (first) {
            GateStep(start = base, commands = invite)
        } else {
            val reset = NavigationCommand.NavigateTo(base, clearBackStack = true)
            GateStep(start = null, commands = listOf(reset) + invite)
        }
    }
}

/**
 * The NavHost's first screen: null until Firebase reports the restored session (MainScreen shows nothing), then
 * [home] or the sign-in. Later session changes navigate through [navigationManager].
 */
@Composable
internal fun rememberSessionStart(
    home: NavigationCommand.Destination,
    navigationManager: NavigationManager,
    auth: AuthService = koinInject(),
    tokens: SessionTokens = koinInject(),
    selectedSpace: SelectedSpace = koinInject(),
    pendingInvites: PendingInviteTokenStore = koinInject(),
    connectivity: ConnectivityMonitor = koinInject()
): NavigationCommand.Destination? {
    val scope = rememberCoroutineScope()
    val gate = remember(auth) {
        SessionGate(home, tokens, selectedSpace, pendingInvites, connectivity.reconnections(), scope)
    }
    var start by remember { mutableStateOf<NavigationCommand.Destination?>(null) }
    LaunchedEffect(gate) {
        auth.state.collect { state ->
            val step = gate.onState(state) ?: return@collect
            step.start?.let { start = it }
            // Delivered once the NavHost listens (NavigationLaunchedEffect starts with it)
            step.commands.forEach(navigationManager::navigate)
        }
    }
    return start
}
