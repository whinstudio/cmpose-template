package com.kturker.multiplatform.core.api

/**
 * The backend's paths, relative and without a leading `/`: the client's base URL ends with `/api/v1/`.
 * Invite tokens travel in the body ([InviteToken]), never in a path.
 */
object ApiRoutes {
    const val ME = "me"
    const val PROFILE = "me/profile"
    const val SPACES = "spaces"
    const val INVITE_PREVIEW = "invites/preview"
    const val INVITE_ACCEPT = "invites/accept"

    /** Uploads go here as multipart; the space is a form field, the personal space when left out. */
    const val FILES = "files"

    fun space(id: String) = "spaces/$id"
    fun members(spaceId: String) = "spaces/$spaceId/members"
    fun member(
        spaceId: String,
        userId: String
    ) = "spaces/$spaceId/members/$userId"
    fun leave(spaceId: String) = "spaces/$spaceId/leave"
    fun invites(spaceId: String) = "spaces/$spaceId/invites"
    fun file(id: String) = "files/$id"
}
