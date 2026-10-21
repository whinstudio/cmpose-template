package com.kturker.multiplatform.core.api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiModelsTest {

    @Test
    fun roleAtLeastFollowsViewerEditorOwnerOrder() {
        val expected = mapOf(
            (Role.VIEWER to Role.VIEWER) to true,
            (Role.VIEWER to Role.EDITOR) to false,
            (Role.VIEWER to Role.OWNER) to false,
            (Role.EDITOR to Role.VIEWER) to true,
            (Role.EDITOR to Role.EDITOR) to true,
            (Role.EDITOR to Role.OWNER) to false,
            (Role.OWNER to Role.VIEWER) to true,
            (Role.OWNER to Role.EDITOR) to true,
            (Role.OWNER to Role.OWNER) to true,
        )
        val serverRoles = Role.entries - Role.UNKNOWN
        assertEquals(serverRoles.size * serverRoles.size, expected.size)
        expected.forEach { (pair, result) ->
            val (role, other) = pair
            assertEquals(result, role.atLeast(other), "$role.atLeast($other)")
        }
    }

    @Test
    fun unknownRoleDecodesToUnknown() {
        assertEquals(Role.UNKNOWN, Json.decodeFromString<Role>("\"admin\""))
        assertEquals(Role.OWNER, Json.decodeFromString<Role>("\"owner\""))
    }

    @Test
    fun unknownInviteStateDecodesToUnknown() {
        assertEquals(InviteState.UNKNOWN, Json.decodeFromString<InviteState>("\"paused\""))
        // A newer server's values do not fail the whole response
        val text = """{"spaceName":"Home","inviterName":null,"role":"admin","expiresAt":7,"state":"paused"}"""
        assertEquals(
            InvitePreview(
                spaceName = "Home",
                inviterName = null,
                role = Role.UNKNOWN,
                expiresAt = 7,
                state = InviteState.UNKNOWN,
            ),
            Json.decodeFromString<InvitePreview>(text),
        )
    }

    @Test
    fun unknownIsNeverSent() {
        assertFailsWith<SerializationException> { Json.encodeToString(RoleChange(Role.UNKNOWN)) }
        assertFailsWith<SerializationException> { Json.encodeToString(InviteState.UNKNOWN) }
        assertEquals("""{"role":"viewer"}""", Json.encodeToString(RoleChange(Role.VIEWER)))
    }

    @Test
    fun unknownRoleHasNoRights() {
        assertFalse(Role.UNKNOWN.atLeast(Role.VIEWER))
        assertTrue(Role.VIEWER.atLeast(Role.UNKNOWN))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun descriptorsListOnlyServerValues() {
        fun SerialDescriptor.names() = (0 until elementsCount).map(::getElementName)
        val role = Role.serializer().descriptor
        assertEquals(SerialKind.ENUM, role.kind)
        assertEquals(listOf("owner", "editor", "viewer"), role.names())
        val state = InviteState.serializer().descriptor
        assertEquals(SerialKind.ENUM, state.kind)
        assertEquals(listOf("valid", "used", "expired", "revoked"), state.names())
    }

    @Test
    fun spaceUsesPersonalOnTheWire() {
        val space = Space(id = "s1", name = "Home", role = Role.EDITOR, personal = false, memberCount = 3)
        val text = Json.encodeToString(space)
        assertEquals(
            """{"id":"s1","name":"Home","role":"editor","personal":false,"memberCount":3}""",
            text,
        )
        assertEquals(space, Json.decodeFromString<Space>(text))
    }

    @Test
    fun memberUsesUserIdOnTheWire() {
        val text = """{"userId":"u1","displayName":null,"email":"a@example.com","role":"owner","joinedAt":42}"""
        val member = Json.decodeFromString<Member>(text)
        assertEquals(
            Member(userId = "u1", displayName = null, email = "a@example.com", role = Role.OWNER, joinedAt = 42),
            member,
        )
        assertEquals(text, Json.encodeToString(member))
    }

    @Test
    fun invitePreviewCarriesExpiryAndState() {
        val text = """{"spaceName":"Home","inviterName":null,"role":"viewer","expiresAt":7,"state":"used"}"""
        assertEquals(
            InvitePreview(
                spaceName = "Home",
                inviterName = null,
                role = Role.VIEWER,
                expiresAt = 7,
                state = InviteState.USED,
            ),
            Json.decodeFromString<InvitePreview>(text),
        )
    }

    @Test
    fun inviteStateReadsRevoked() {
        assertEquals(InviteState.REVOKED, Json.decodeFromString<InviteState>("\"revoked\""))
    }

    @Test
    fun inviteLinkCarriesItsId() {
        val text = """{"id":"i1","token":"t1","expiresAt":9}"""
        assertEquals(
            InviteLink(id = "i1", token = "t1", expiresAt = 9),
            Json.decodeFromString<InviteLink>(text),
        )
    }

    @Test
    fun pendingInviteReadsRoleAndTimes() {
        val text = """{"id":"i1","role":"editor","createdAt":3,"expiresAt":9}"""
        val invite = Json.decodeFromString<PendingInvite>(text)
        assertEquals(PendingInvite(id = "i1", role = Role.EDITOR, createdAt = 3, expiresAt = 9), invite)
        assertEquals(text, Json.encodeToString(invite))
    }

    @Test
    fun errorBodyReadsTheQuota() {
        val text = """{"error":{"code":"QUOTA_EXCEEDED","message":"m","requestId":"r","quota":"members"}}"""
        val error = Json.decodeFromString<ErrorBody>(text).error
        assertEquals(ErrorCodes.QUOTA_EXCEEDED, error.code)
        assertEquals("members", error.quota)
    }

    @Test
    fun errorBodyReadsTheNestedEnvelope() {
        val validation = """{"error":{"code":"BAD_REQUEST","message":"Invalid input","requestId":"r1",""" +
            """"fields":[{"field":"name","message":"Too long"}]}}"""
        assertEquals(
            ErrorBody(
                ErrorDetail(
                    code = ErrorCodes.BAD_REQUEST,
                    message = "Invalid input",
                    requestId = "r1",
                    fields = listOf(FieldError(field = "name", message = "Too long")),
                ),
            ),
            Json.decodeFromString<ErrorBody>(validation),
        )
        val upgrade = """{"error":{"code":"CLIENT_UPGRADE_REQUIRED","message":"Update","requestId":"r2",""" +
            """"minVersion":"2.1.0"}}"""
        assertEquals("2.1.0", Json.decodeFromString<ErrorBody>(upgrade).error.minVersion)
        // requestId, fields and minVersion are optional on the client: a body without them still reads
        val bare = Json.decodeFromString<ErrorBody>("""{"error":{"code":"NOT_FOUND","message":"Not found"}}""")
        assertEquals(ErrorDetail(code = ErrorCodes.NOT_FOUND, message = "Not found"), bare.error)
    }

    @Test
    fun errorCodesAreTheirOwnNames() {
        val codes = mapOf(
            ErrorCodes.NOT_FOUND to "NOT_FOUND",
            ErrorCodes.UNAUTHORIZED to "UNAUTHORIZED",
            ErrorCodes.BAD_REQUEST to "BAD_REQUEST",
            ErrorCodes.RATE_LIMITED to "RATE_LIMITED",
            ErrorCodes.CLIENT_UPGRADE_REQUIRED to "CLIENT_UPGRADE_REQUIRED",
            ErrorCodes.INVITE_USED to "INVITE_USED",
            ErrorCodes.INVITE_EXPIRED to "INVITE_EXPIRED",
            ErrorCodes.LAST_OWNER to "LAST_OWNER",
            ErrorCodes.QUOTA_EXCEEDED to "QUOTA_EXCEEDED",
            ErrorCodes.INVITE_REVOKED to "INVITE_REVOKED",
        )
        codes.forEach { (value, name) -> assertEquals(name, value) }
    }

    @Test
    fun routesHaveNoLeadingSlash() {
        val routes = listOf(
            ApiRoutes.ME,
            ApiRoutes.PROFILE,
            ApiRoutes.SPACES,
            ApiRoutes.space("s1"),
            ApiRoutes.members("s1"),
            ApiRoutes.member("s1", "u1"),
            ApiRoutes.leave("s1"),
            ApiRoutes.invites("s1"),
            ApiRoutes.invite("s1", "i1"),
            ApiRoutes.INVITE_PREVIEW,
            ApiRoutes.INVITE_ACCEPT,
            ApiRoutes.FILES,
            ApiRoutes.file("f1"),
        )
        routes.forEach { assertFalse(it.startsWith("/"), "route '$it' starts with '/'") }
    }

    @Test
    fun routesBuildIdPaths() {
        assertEquals("spaces/s1", ApiRoutes.space("s1"))
        assertEquals("spaces/s1/members", ApiRoutes.members("s1"))
        assertEquals("spaces/s1/members/u1", ApiRoutes.member("s1", "u1"))
        assertEquals("spaces/s1/leave", ApiRoutes.leave("s1"))
        assertEquals("spaces/s1/invites", ApiRoutes.invites("s1"))
        assertEquals("spaces/s1/invites/i1", ApiRoutes.invite("s1", "i1"))
        assertEquals("files/f1", ApiRoutes.file("f1"))
    }

    @Test
    fun inviteRoutesCarryNoToken() {
        // The token travels in the body (InviteToken), never in the path, so it stays out of access logs
        assertEquals("invites/preview", ApiRoutes.INVITE_PREVIEW)
        assertEquals("invites/accept", ApiRoutes.INVITE_ACCEPT)
        assertEquals(
            """{"token":"t1"}""",
            Json.encodeToString(InviteToken(token = "t1")),
        )
    }
}
