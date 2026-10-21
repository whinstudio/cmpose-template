package com.kturker.multiplatform.feature.sharing.presentation

import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.InviteLink
import com.kturker.multiplatform.core.api.InvitePreview
import com.kturker.multiplatform.core.api.InviteRequest
import com.kturker.multiplatform.core.api.InviteState
import com.kturker.multiplatform.core.api.InviteToken
import com.kturker.multiplatform.core.api.Me
import com.kturker.multiplatform.core.api.Member
import com.kturker.multiplatform.core.api.PendingInvite
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.RoleChange
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.api.SpaceDraft
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.network.TokenSource
import com.kturker.multiplatform.core.network.buildBackendClient
import com.kturker.multiplatform.feature.sharing.presentation.data.KtorSpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SpacesRepositoryTest {

    private object Tokens : TokenSource {
        override suspend fun token(forceRefresh: Boolean): String? = "t"
    }

    private val requests = mutableListOf<HttpRequestData>()

    // The base URL ends in the API prefix: a path starting with "/" would escape it, so every call must stay under it
    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): SpacesRepository {
        val engine = MockEngine { request ->
            requests += request
            handler(request)
        }
        val client = buildBackendClient(
            engine = engine,
            baseUrl = BASE,
            tokens = Tokens,
            isDebug = false,
            clientVersion = "1.0.0",
            onUpgradeRequired = {}
        )
        return KtorSpacesRepository(client)
    }

    // On real time: runTest's virtual clock would fire HttpTimeout before MockEngine answers on another dispatcher
    private fun apiTest(block: suspend CoroutineScope.() -> Unit): TestResult =
        runTest { withContext(Dispatchers.Default) { block() } }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun MockRequestHandleScope.noContent() = respond("", HttpStatusCode.NoContent)

    private suspend inline fun <reified T> HttpRequestData.decodedBody(): T =
        Json.decodeFromString(body.toByteArray().decodeToString())

    private fun lastCall() = requests.last().let { it.method to it.url.toString() }

    @Test
    fun everyCallStaysUnderTheApiPrefix() = apiTest {
        val spaceJson = """{"id":"s1","name":"Home","role":"owner","personal":false,"memberCount":2}"""
        val previewJson = """{"spaceName":"Home","inviterName":null,"role":"editor","expiresAt":5,"state":"valid"}"""

        repository { json("[$spaceJson]") }.list()
        assertEquals(HttpMethod.Get to "${BASE}spaces", lastCall())

        repository { json(spaceJson, HttpStatusCode.Created) }.create("Home")
        assertEquals(HttpMethod.Post to "${BASE}spaces", lastCall())

        repository { json(spaceJson) }.rename("s1", "Home")
        assertEquals(HttpMethod.Patch to "${BASE}spaces/s1", lastCall())

        repository { noContent() }.delete("s1")
        assertEquals(HttpMethod.Delete to "${BASE}spaces/s1", lastCall())

        repository { json("[]") }.members("s1")
        assertEquals(HttpMethod.Get to "${BASE}spaces/s1/members", lastCall())

        repository { noContent() }.setRole("s1", "u2", Role.VIEWER)
        assertEquals(HttpMethod.Patch to "${BASE}spaces/s1/members/u2", lastCall())

        repository { noContent() }.remove("s1", "u2")
        assertEquals(HttpMethod.Delete to "${BASE}spaces/s1/members/u2", lastCall())

        repository { noContent() }.leave("s1")
        assertEquals(HttpMethod.Post to "${BASE}spaces/s1/leave", lastCall())

        repository { json("""{"token":"tok","expiresAt":5}""", HttpStatusCode.Created) }.invite("s1", Role.EDITOR)
        assertEquals(HttpMethod.Post to "${BASE}spaces/s1/invites", lastCall())

        repository { json("[]") }.pendingInvites("s1")
        assertEquals(HttpMethod.Get to "${BASE}spaces/s1/invites", lastCall())

        repository { noContent() }.revokeInvite("s1", "i1")
        assertEquals(HttpMethod.Delete to "${BASE}spaces/s1/invites/i1", lastCall())

        repository { json(previewJson) }.preview("tok")
        assertEquals(HttpMethod.Post to "${BASE}invites/preview", lastCall())

        repository { json(spaceJson) }.accept("tok")
        assertEquals(HttpMethod.Post to "${BASE}invites/accept", lastCall())

        repository { json("""{"id":"b1"}""") }.me()
        assertEquals(HttpMethod.Get to "${BASE}me", lastCall())

        requests.forEach { assertTrue(it.url.encodedPath.startsWith("/api/v1/"), it.url.toString()) }
    }

    // The token stays out of paths, and so out of access logs: preview and accept send it as InviteToken
    @Test
    fun inviteTokenTravelsInTheBody() = apiTest {
        val preview = repository {
            json("""{"spaceName":"Home","inviterName":"Ada","role":"editor","expiresAt":7,"state":"used"}""")
        }.preview("tok-1")
        assertEquals(InviteToken("tok-1"), requests.last().decodedBody<InviteToken>())
        assertFalse(requests.last().url.toString().contains("tok-1"))
        assertEquals(InvitePreview("Home", "Ada", Role.EDITOR, expiresAt = 7, state = InviteState.USED), preview.success())

        val joined = repository {
            json("""{"id":"s1","name":"Home","role":"editor","personal":false,"memberCount":2}""")
        }.accept("tok-2")
        assertEquals(InviteToken("tok-2"), requests.last().decodedBody<InviteToken>())
        assertFalse(requests.last().url.toString().contains("tok-2"))
        assertEquals(Space("s1", "Home", Role.EDITOR, personal = false, memberCount = 2), joined.success())
    }

    @Test
    fun leavePostsToLeave() = apiTest {
        val result = repository { noContent() }.leave("s1")

        val request = requests.single()
        assertEquals(HttpMethod.Post to "${BASE}spaces/s1/leave", request.method to request.url.toString())
        assertIs<RestResult.Success<Unit>>(result)
    }

    // The members list marks "(you)" by the backend's own user id, never the sign-in provider's uid
    @Test
    fun meReadsTheBackendUserId() = apiTest {
        val me = repository { json("""{"id":"b1","email":"a@example.com","displayName":null}""") }.me()
        assertEquals(Me(id = "b1", email = "a@example.com", displayName = null), me.success())

        // Only the id is always in the body
        assertEquals(Me(id = "b2"), repository { json("""{"id":"b2"}""") }.me().success())
    }

    @Test
    fun bodiesAndAnswersAreTheSharedModels() = apiTest {
        val created = repository {
            json("""{"id":"s1","name":"Home","role":"owner","personal":false,"memberCount":1}""")
        }.create("Home")
        assertEquals(SpaceDraft("Home"), requests.last().decodedBody<SpaceDraft>())
        assertEquals(Space("s1", "Home", Role.OWNER, personal = false, memberCount = 1), created.success())

        repository { noContent() }.setRole("s1", "u2", Role.VIEWER)
        assertEquals(RoleChange(Role.VIEWER), requests.last().decodedBody<RoleChange>())

        val link = repository { json("""{"id":"i1","token":"tok","expiresAt":5}""") }.invite("s1", Role.EDITOR)
        assertEquals(InviteRequest(Role.EDITOR), requests.last().decodedBody<InviteRequest>())
        assertEquals(InviteLink("i1", "tok", 5), link.success())

        val members = repository {
            json("""[{"userId":"u2","displayName":null,"email":"b@example.com","role":"viewer","joinedAt":3}]""")
        }.members("s1")
        assertEquals(listOf(Member("u2", null, "b@example.com", Role.VIEWER, 3)), members.success())

        val invites = repository {
            json("""[{"id":"i1","role":"editor","createdAt":3,"expiresAt":9}]""")
        }.pendingInvites("s1")
        assertEquals(listOf(PendingInvite("i1", Role.EDITOR, createdAt = 3, expiresAt = 9)), invites.success())
    }

    // A used invite answers 409, an expired one 410: both reach the screen as HTTP errors with the backend's code
    @Test
    fun usedAndExpiredInvitesAreHttpErrors() = apiTest {
        fun error(code: String) = """{"error":{"code":"$code","message":"x","requestId":"r1"}}"""

        val used = repository { json(error("INVITE_USED"), HttpStatusCode.Conflict) }.accept("tok")
        assertEquals(DataError.Http(409, ErrorCodes.INVITE_USED), assertIs<RestResult.Error>(used).error)

        val expired = repository { json(error("INVITE_EXPIRED"), HttpStatusCode.Gone) }.accept("tok")
        assertEquals(DataError.Http(410, ErrorCodes.INVITE_EXPIRED), assertIs<RestResult.Error>(expired).error)

        val unknown = repository { json(error("NOT_FOUND"), HttpStatusCode.NotFound) }.preview("tok")
        assertEquals(DataError.Http(404, ErrorCodes.NOT_FOUND), assertIs<RestResult.Error>(unknown).error)
    }

    private fun <T : Any> RestResult<T>.success(): T = assertIs<RestResult.Success<T>>(this).result

    private companion object {
        const val BASE = "https://example.com/api/v1/"
    }
}
