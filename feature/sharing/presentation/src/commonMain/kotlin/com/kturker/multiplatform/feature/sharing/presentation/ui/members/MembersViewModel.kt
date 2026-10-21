package com.kturker.multiplatform.feature.sharing.presentation.ui.members

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.PendingInvite
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.DEEP_LINK_SCHEME
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.sharing.contract.MembersDestination
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.data.inviteUrl
import com.kturker.multiplatform.feature.sharing.presentation.share.InviteText
import com.kturker.multiplatform.feature.sharing.presentation.share.TextSharer
import com.kturker.multiplatform.feature.sharing.presentation.ui.BackendFailures
import com.kturker.multiplatform.feature.sharing.presentation.ui.HTTP_NOT_FOUND
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError
import com.kturker.multiplatform.feature.sharing.presentation.ui.apiCode
import com.kturker.multiplatform.feature.sharing.presentation.ui.httpCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/**
 * A space's members. The owner changes roles, removes members and shares invite links: Share asks for the role,
 * creates a single-use link (POST spaces/{id}/invites) and hands "<text>: <link>" to the platform's share sheet.
 * The owner also sees the links not yet used (GET spaces/{id}/invites) and revokes one after a confirmation.
 * "(you)" marks the member whose id is the backend's id for the signed-in user (GET me).
 */
@KoinViewModel
internal class MembersViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: SpacesRepository,
    @Provided private val auth: AuthService,
    @Provided private val tokens: SessionTokens,
    private val selectedSpace: SelectedSpace,
    private val sharer: TextSharer,
    private val inviteText: InviteText,
    @Provided private val navigationManager: NavigationManager
) : CoreViewModel(),
    MembersActions {

    // The route's argument, read by its name: toRoute<MembersDestination>() decodes it through an Android Bundle,
    // which the JVM unit tests (testAndroidHostTest) do not have
    private val spaceId: String =
        checkNotNull(savedStateHandle[MembersDestination::spaceId.name]) { "spaceId missing" }

    private val _state = MutableStateFlow(MembersUiState())
    val state: StateFlow<MembersUiState> = _state.asStateFlow()

    private val failures = BackendFailures(auth, tokens, selectedSpace)

    init {
        viewModelScope.launch { load() }
    }

    override fun onBack() = navigationManager.navigate(NavigationCommand.NavigateUp)

    override fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { load() }
    }

    override fun startShare() = _state.update { it.copy(choosingInviteRole = true, error = null) }

    override fun dismissShare() = _state.update { it.copy(choosingInviteRole = false) }

    // The new link joins the pending list; too many pending links answer 409 QUOTA_EXCEEDED
    override fun share(role: Role) = act(reload = true, quota = SharingError.INVITE_LIMIT) {
        val link = repository.invite(spaceId, role)
        if (link is RestResult.Success) {
            sharer.share(inviteText.of(inviteUrl(scheme = DEEP_LINK_SCHEME, token = link.result.token)))
        }
        link
    }

    override fun changeRole(
        member: Member,
        role: Role
    ) = act(reload = true) { repository.setRole(spaceId, member.userId, role) }

    override fun requestRemove(member: Member) =
        _state.update { it.copy(removing = member, error = null) }

    override fun dismissRemove() = _state.update { it.copy(removing = null) }

    override fun confirmRemove() {
        val member = _state.value.removing ?: return
        act(reload = true) { repository.remove(spaceId, member.userId) }
    }

    override fun requestRevoke(invite: PendingInvite) =
        _state.update { it.copy(revoking = invite, error = null) }

    override fun dismissRevoke() = _state.update { it.copy(revoking = null) }

    override fun confirmRevoke() {
        val invite = _state.value.revoking ?: return
        act(reload = true) {
            val result = repository.revokeInvite(spaceId, invite.id)
            // Someone joined with the link meanwhile (409 INVITE_USED): it is no longer pending either, so the
            // reload shows the list as it is now. A 404 (the invite or the space is gone) reloads in act too.
            val used = result is RestResult.Error && result.error.apiCode == ErrorCodes.INVITE_USED
            if (used) RestResult.Success(Unit) else result
        }
    }

    // quota: what 409 QUOTA_EXCEEDED means for this call (BackendFailures.handle)
    private fun act(
        reload: Boolean = false,
        quota: SharingError = SharingError.UNKNOWN,
        call: suspend () -> RestResult<Any>
    ) {
        if (_state.value.busy) return
        _state.update {
            it.copy(busy = true, choosingInviteRole = false, removing = null, revoking = null, error = null)
        }
        viewModelScope.launch {
            val result = call()
            val failed = result is RestResult.Error
            // A 404: the space is gone, or only the target member is (they left meanwhile). load() tells the two
            // apart through GET spaces and otherwise shows the current list.
            if (failed && result.error.httpCode != HTTP_NOT_FOUND) {
                val error = failures.handle(result.error, quota)
                _state.update { it.copy(busy = false, error = error) }
                return@launch
            }
            if (reload || failed) load()
            _state.update { it.copy(busy = false) }
        }
    }

    // The space (its name, the user's role) comes from GET spaces; one missing from it is gone for this user
    private suspend fun load() {
        loadMe()
        val spaces = repository.list()
        if (spaces is RestResult.Error) return fail(spaces)
        val space = (spaces as? RestResult.Success)?.result?.firstOrNull { it.id == spaceId }
        if (space == null) {
            val error = failures.noAccess()
            return _state.update { it.copy(loading = false, error = error) }
        }
        when (val members = repository.members(spaceId)) {
            is RestResult.Success -> {
                _state.update { it.copy(space = space, members = members.result) }
                loadInvites()
            }

            is RestResult.Error -> fail(members)

            is RestResult.Loading -> Unit
        }
    }

    // Only the owner of a sharing app asks: the server answers anyone else 404, which would read as no access
    private suspend fun loadInvites() {
        if (!_state.value.canShare) {
            return _state.update { it.copy(invites = emptyList(), loading = false, error = null) }
        }
        when (val invites = repository.pendingInvites(spaceId)) {
            is RestResult.Success -> _state.update {
                it.copy(invites = invites.result, loading = false, error = null)
            }

            is RestResult.Error -> fail(invites)

            is RestResult.Loading -> Unit
        }
    }

    // Asked until it answers; a failure only leaves "(you)" out, as GET spaces reports the error itself
    private suspend fun loadMe() {
        if (_state.value.myUserId != null) return
        val me = repository.me() as? RestResult.Success ?: return
        _state.update { it.copy(myUserId = me.result.id) }
    }

    private suspend fun fail(result: RestResult.Error) {
        val error = failures.handle(result.error)
        _state.update { it.copy(loading = false, error = error) }
    }
}
