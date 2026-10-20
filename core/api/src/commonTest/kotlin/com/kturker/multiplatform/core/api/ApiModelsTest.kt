package com.kturker.multiplatform.core.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

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
        assertEquals(Role.entries.size * Role.entries.size, expected.size)
        expected.forEach { (pair, result) ->
            val (role, other) = pair
            assertEquals(result, role.atLeast(other), "$role.atLeast($other)")
        }
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
