package com.kturker.multiplatform.feature.account.presentation

import com.kturker.multiplatform.core.api.ErrorCodes
import com.kturker.multiplatform.core.api.Profile
import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.network.TokenSource
import com.kturker.multiplatform.core.network.buildBackendClient
import com.kturker.multiplatform.feature.account.presentation.data.AccountRepository
import com.kturker.multiplatform.feature.account.presentation.data.KtorAccountRepository
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
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AccountRepositoryTest {

    private object Tokens : TokenSource {
        override suspend fun token(forceRefresh: Boolean): String? = "t"
    }

    private val requests = mutableListOf<HttpRequestData>()

    // The base URL ends in the API prefix: request paths must stay under it
    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): AccountRepository {
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
        return KtorAccountRepository(client)
    }

    // On real time: runTest's virtual clock would fire HttpTimeout before MockEngine answers on another dispatcher
    private fun apiTest(block: suspend CoroutineScope.() -> Unit): TestResult =
        runTest { withContext(Dispatchers.Default) { block() } }

    private fun MockRequestHandleScope.json(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun spacesReadsTheListUnderTheApiPrefix() = apiTest {
        val result = repository {
            json("""[{"id":"s1","name":"Home","role":"owner","personal":false,"memberCount":3}]""")
        }.spaces()

        assertEquals("${BASE}spaces", requests.single().url.toString())
        assertTrue(requests.single().url.encodedPath.startsWith("/api/v1/"))
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals(
            listOf(Space("s1", "Home", Role.OWNER, personal = false, memberCount = 3)),
            assertIs<RestResult.Success<List<Space>>>(result).result
        )
    }

    // The backend replaces the display name on every PUT (a missing one clears it): both fields always go
    @Test
    fun updateProfilePutsTheProfile() = apiTest {
        val result = repository { respond("", HttpStatusCode.NoContent) }
            .updateProfile(Profile(displayName = "Ada", personalSpaceName = "Personal"))

        val request = requests.single()
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("${BASE}me/profile", request.url.toString())
        assertTrue(request.url.encodedPath.startsWith("/api/v1/"))
        val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        assertEquals(setOf("displayName", "personalSpaceName"), body.keys)
        assertEquals(
            Profile(displayName = "Ada", personalSpaceName = "Personal"),
            Json.decodeFromJsonElement<Profile>(body)
        )
        assertIs<RestResult.Success<Unit>>(result)
    }

    // Without a name (an email account) the profile still names the personal space; the backend clears the name
    @Test
    fun updateProfileWithoutADisplayNameStillNamesThePersonalSpace() = apiTest {
        repository { respond("", HttpStatusCode.NoContent) }
            .updateProfile(Profile(displayName = null, personalSpaceName = "Personal"))

        val body = Json.decodeFromString<Profile>(requests.single().body.toByteArray().decodeToString())
        assertEquals(Profile(displayName = null, personalSpaceName = "Personal"), body)
    }

    @Test
    fun deleteServerAccountDeletesMe() = apiTest {
        val result = repository { respond("", HttpStatusCode.NoContent) }.deleteServerAccount()

        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("${BASE}me", requests.single().url.toString())
        assertTrue(requests.single().url.encodedPath.startsWith("/api/v1/"))
        assertIs<RestResult.Success<Unit>>(result)
    }

    @Test
    fun aServerErrorIsAnHttpError() = apiTest {
        val result = repository { respond("", HttpStatusCode.InternalServerError) }.deleteServerAccount()

        assertEquals(DataError.Http(500), assertIs<RestResult.Error>(result).error)
    }

    // The backend's error body fills in its code
    @Test
    fun aRateLimitedAnswerCarriesItsCode() = apiTest {
        val result = repository {
            respond(
                """{"error":{"code":"RATE_LIMITED","message":"Too many requests","requestId":"r1"}}""",
                HttpStatusCode.TooManyRequests,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }.deleteServerAccount()

        assertEquals(DataError.Http(429, ErrorCodes.RATE_LIMITED), assertIs<RestResult.Error>(result).error)
    }

    private companion object {
        const val BASE = "https://example.com/api/v1/"
    }
}
