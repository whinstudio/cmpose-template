package com.kturker.multiplatform.feature.sharing.presentation.ui.members

import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.feature.sharing.contract.SharingConfig
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError

internal data class MembersUiState(
    val space: Space? = null,
    val members: List<Member> = emptyList(),
    /** The signed-in user's backend id (GET me), marked "(you)" in the list; null until the backend answers. */
    val myUserId: String? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    /** The role picker shown before an invite link is made. */
    val choosingInviteRole: Boolean = false,
    /** The member whose removal waits for a confirmation. */
    val removing: Member? = null,
    val error: SharingError? = null
) {
    private val isOwner: Boolean get() = space?.role == Role.OWNER

    /**
     * Only the owner invites (the server refuses anyone else), and only in an app that shares. The personal space
     * can be shared too; it only cannot be deleted.
     */
    val canShare: Boolean get() = isOwner && SharingConfig.ENABLED

    /** The owner changes the role of, and removes, anyone but themselves; the owner's own role never changes. */
    fun canManage(member: Member): Boolean = isOwner && member.role != Role.OWNER

    /** The signed-in user themselves, by the backend's id: the sign-in provider's uid is never a [Member.userId]. */
    fun isYou(member: Member): Boolean = myUserId != null && member.userId == myUserId

    companion object {
        /** The roles an invite or a role change can give: never OWNER. */
        val grantableRoles = listOf(Role.EDITOR, Role.VIEWER)
    }
}
