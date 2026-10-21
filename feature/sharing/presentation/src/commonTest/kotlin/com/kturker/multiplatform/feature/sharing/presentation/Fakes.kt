package com.kturker.multiplatform.feature.sharing.presentation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.kturker.multiplatform.core.api.InviteLink
import com.kturker.multiplatform.core.api.InvitePreview
import com.kturker.multiplatform.core.api.Me
import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.PendingInvite
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.auth.AuthResult
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.AuthState
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.auth.SignOutReason
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.share.TextSharer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

internal fun space(
    id: String,
    role: Role = Role.OWNER,
    personal: Boolean = false,
    memberCount: Int = 1,
    name: String = id,
) = Space(id = id, name = name, role = role, personal = personal, memberCount = memberCount)

internal fun member(
    userId: String,
    role: Role = Role.EDITOR,
    displayName: String? = userId,
    email: String? = null,
) = Member(userId = userId, displayName = displayName, email = email, role = role, joinedAt = 0)

/** Only what the sharing screens use; each sign-out is appended to [log] with its reason. */
internal class FakeAuthService(
    val log: MutableList<String> = mutableListOf(),
    initial: AuthState = AuthState.SignedIn("me", "me@example.com"),
) : AuthService {
    override val state = MutableStateFlow(initial)

    override fun currentUid(): String? = (state.value as? AuthState.SignedIn)?.uid

    override fun currentDisplayName(): String? = null

    override suspend fun token(forceRefresh: Boolean): String? = "token"

    override suspend fun signInWithEmail(email: String, password: String): AuthResult = AuthResult.Success

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult = AuthResult.Success

    override suspend fun signInWithGoogle(): AuthResult = AuthResult.Success

    override suspend fun signInWithApple(): AuthResult = AuthResult.Success

    override suspend fun signOut(reason: SignOutReason) {
        log += "signOut:$reason"
        state.value = AuthState.SignedOut
    }

    override fun takeSessionEndedNotice(): Boolean = false

    override suspend fun deleteUser(): AuthResult = AuthResult.Success

    override suspend fun canDeleteNow(): AuthResult = AuthResult.Success
}

internal class FakeSessionTokens(private val log: MutableList<String>) : SessionTokens {
    override fun forget() {
        log += "forgetToken"
    }
}

/** Every call is appended to [log] (name and arguments); each result is a settable field. */
internal class FakeSpacesRepository(val log: MutableList<String> = mutableListOf()) : SpacesRepository {
    var listResult: RestResult<List<Space>> = RestResult.Success(emptyList())
    var createResult: RestResult<Space> = RestResult.Success(space("new"))
    var renameResult: RestResult<Space> = RestResult.Success(space("renamed"))
    var deleteResult: RestResult<Unit> = RestResult.Success(Unit)
    var membersResult: RestResult<List<Member>> = RestResult.Success(emptyList())
    var setRoleResult: RestResult<Unit> = RestResult.Success(Unit)
    var removeResult: RestResult<Unit> = RestResult.Success(Unit)
    var leaveResult: RestResult<Unit> = RestResult.Success(Unit)
    var inviteResult: RestResult<InviteLink> = RestResult.Success(InviteLink(id = "i1", token = "tok", expiresAt = 0))
    var previewResult: RestResult<InvitePreview>? = null
    var acceptResult: RestResult<Space> = RestResult.Success(space("joined", role = Role.EDITOR))
    var pendingInvitesResult: RestResult<List<PendingInvite>> = RestResult.Success(emptyList())
    var revokeInviteResult: RestResult<Unit> = RestResult.Success(Unit)

    /** The backend's own id for the signed-in user, unlike FakeAuthService's Firebase uid ("me"). */
    var meResult: RestResult<Me> = RestResult.Success(Me(id = "backend-me"))

    override suspend fun list(): RestResult<List<Space>> = listResult.also { log += "list" }

    override suspend fun create(name: String): RestResult<Space> = createResult.also { log += "create:$name" }

    override suspend fun rename(id: String, name: String): RestResult<Space> =
        renameResult.also { log += "rename:$id:$name" }

    override suspend fun delete(id: String): RestResult<Unit> = deleteResult.also { log += "delete:$id" }

    override suspend fun members(id: String): RestResult<List<Member>> = membersResult.also { log += "members:$id" }

    override suspend fun setRole(spaceId: String, userId: String, role: Role): RestResult<Unit> =
        setRoleResult.also { log += "setRole:$spaceId:$userId:$role" }

    override suspend fun remove(spaceId: String, userId: String): RestResult<Unit> =
        removeResult.also { log += "remove:$spaceId:$userId" }

    override suspend fun leave(spaceId: String): RestResult<Unit> = leaveResult.also { log += "leave:$spaceId" }

    override suspend fun invite(id: String, role: Role): RestResult<InviteLink> =
        inviteResult.also { log += "invite:$id:$role" }

    override suspend fun preview(token: String): RestResult<InvitePreview> =
        checkNotNull(previewResult) { "no preview result set" }.also { log += "preview:$token" }

    override suspend fun accept(token: String): RestResult<Space> = acceptResult.also { log += "accept:$token" }

    override suspend fun me(): RestResult<Me> = meResult.also { log += "me" }

    override suspend fun pendingInvites(spaceId: String): RestResult<List<PendingInvite>> =
        pendingInvitesResult.also { log += "pendingInvites:$spaceId" }

    override suspend fun revokeInvite(spaceId: String, inviteId: String): RestResult<Unit> =
        revokeInviteResult.also { log += "revokeInvite:$spaceId:$inviteId" }
}

/** Records select and refresh in [log]; [current] is set by the test. */
internal class FakeSelectedSpace(private val log: MutableList<String>) : SelectedSpace {
    override val current: MutableStateFlow<Space?> = MutableStateFlow(null)

    override suspend fun select(spaceId: String) {
        log += "select:$spaceId"
    }

    override suspend fun refresh() {
        log += "refresh"
    }

    override suspend fun clear() {
        log += "clear"
    }
}

internal class RecordingSharer : TextSharer {
    val shared = mutableListOf<String>()

    override fun share(text: String) {
        shared += text
    }
}

internal class RecordingNavigationManager : NavigationManager {
    val commands = mutableListOf<NavigationCommand>()
    override val navigationCommandFlow: Flow<NavigationCommand> = emptyFlow()

    override fun navigate(navigationCommand: NavigationCommand) {
        commands += navigationCommand
    }
}

internal class InMemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: StateFlow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}
