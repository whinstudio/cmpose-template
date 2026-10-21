package com.kturker.multiplatform.core.api

import kotlinx.serialization.Serializable

// The backend's API contract: each model matches the schema of the same meaning in its OpenAPI description
// (ApiContractTest checks them against server/build/openapi/openapi.json).

/**
 * On the wire each role is its name in lower case. [UNKNOWN] is any role a newer server sends that the app does
 * not know; it is never sent.
 */
@Serializable(with = RoleSerializer::class)
enum class Role {
    OWNER,
    EDITOR,
    VIEWER,
    UNKNOWN
}

internal object RoleSerializer : FallbackEnumSerializer<Role>(
    serialName = "com.kturker.multiplatform.core.api.Role",
    entries = Role.entries,
    fallback = Role.UNKNOWN,
    wireName = { it.name.lowercase() }
)

/** Role order for checks: UNKNOWN < VIEWER < EDITOR < OWNER; a role the app does not know grants nothing. */
fun Role.atLeast(other: Role): Boolean = rank() >= other.rank()

private fun Role.rank() = when (this) {
    Role.UNKNOWN -> -1
    Role.VIEWER -> 0
    Role.EDITOR -> 1
    Role.OWNER -> 2
}

/** A space as the caller sees it: their [role] in it and how many members it has (schema `Space`). */
@Serializable
data class Space(val id: String, val name: String, val role: Role, val personal: Boolean, val memberCount: Int)

@Serializable
data class SpaceDraft(val name: String)

/** [displayName] and [email] are null when the member has not set or verified one (schema `Member`). */
@Serializable
data class Member(val userId: String, val displayName: String?, val email: String?, val role: Role, val joinedAt: Long)

@Serializable
data class RoleChange(val role: Role)

@Serializable
data class InviteRequest(val role: Role)

/** A new invite (schema `Invite`); its [token] is shown only once, when it is created. */
@Serializable
data class InviteLink(val id: String, val token: String, val expiresAt: Long)

/** An invite of the space not yet accepted, revoked or run out, as its owner sees it (schema `PendingInvite`). */
@Serializable
data class PendingInvite(val id: String, val role: Role, val createdAt: Long, val expiresAt: Long)

/** The body of the invite preview and accept calls: the token never goes in the path. */
@Serializable
data class InviteToken(val token: String)

/**
 * On the wire each state is its name in lower case. [UNKNOWN] is any state a newer server sends that the app does
 * not know; it is never sent.
 */
@Serializable(with = InviteStateSerializer::class)
enum class InviteState {
    VALID,
    USED,
    EXPIRED,
    REVOKED,
    UNKNOWN
}

internal object InviteStateSerializer : FallbackEnumSerializer<InviteState>(
    serialName = "com.kturker.multiplatform.core.api.InviteState",
    entries = InviteState.entries,
    fallback = InviteState.UNKNOWN,
    wireName = { it.name.lowercase() }
)

@Serializable
data class InvitePreview(
    val spaceName: String,
    val inviterName: String?,
    val role: Role,
    val expiresAt: Long,
    val state: InviteState
)

/** The signed-in user as the server knows them; only [id] is always in the body (schema `Me`). */
@Serializable
data class Me(val id: String, val email: String? = null, val displayName: String? = null)

/**
 * The body of `PUT me/profile` (schema `ProfileUpdate`). A null [displayName] clears the display name;
 * a null [personalSpaceName] leaves the personal space's name as it is.
 */
@Serializable
data class Profile(val displayName: String? = null, val personalSpaceName: String? = null)

/** A stored file (schema `File`). */
@Serializable
data class FileInfo(val id: String, val mime: String, val size: Long)

/** One invalid input field of a 400 `BAD_REQUEST`. */
@Serializable
data class FieldError(val field: String, val message: String)

/**
 * [code] is one of [ErrorCodes] (or another constant); [minVersion] comes only with `CLIENT_UPGRADE_REQUIRED`,
 * [quota] (which quota is full) only with `QUOTA_EXCEEDED`.
 */
@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val requestId: String? = null,
    val fields: List<FieldError>? = null,
    val minVersion: String? = null,
    val quota: String? = null
)

/** Every error the server sends: `{"error":{"code":…,"message":…,"requestId":…}}` (schema `Error`). */
@Serializable
data class ErrorBody(val error: ErrorDetail)

/** The [ErrorDetail.code] values the client acts on. */
object ErrorCodes {
    const val NOT_FOUND = "NOT_FOUND"
    const val UNAUTHORIZED = "UNAUTHORIZED"
    const val BAD_REQUEST = "BAD_REQUEST"
    const val RATE_LIMITED = "RATE_LIMITED"

    /** HTTP 426: the app is older than the server's minimum; see [ErrorDetail.minVersion]. */
    const val CLIENT_UPGRADE_REQUIRED = "CLIENT_UPGRADE_REQUIRED"

    /** HTTP 409: the invite has already been accepted. */
    const val INVITE_USED = "INVITE_USED"

    /** HTTP 410: the invite has run out. */
    const val INVITE_EXPIRED = "INVITE_EXPIRED"

    /** HTTP 409: the space's last owner cannot leave, be removed or lose the owner role. */
    const val LAST_OWNER = "LAST_OWNER"

    /** HTTP 409: a quota is full; [ErrorDetail.quota] says which. */
    const val QUOTA_EXCEEDED = "QUOTA_EXCEEDED"

    /** HTTP 410: the owner revoked the invite. */
    const val INVITE_REVOKED = "INVITE_REVOKED"
}
