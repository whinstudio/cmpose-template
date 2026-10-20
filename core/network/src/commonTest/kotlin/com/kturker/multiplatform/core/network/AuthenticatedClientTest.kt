package com.kturker.multiplatform.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private const val CLIENT_VERSION = "1.2.3"
private const val CLIENT_VERSION_HEADER = "X-Client-Version"

class AuthenticatedClientTest {

    private class FakeTokens(var current: String?, val refreshed: String?) : TokenSource {
        var refreshCalls = 0
        override suspend fun token(forceRefresh: Boolean): String? =
            if (forceRefresh) {
                refreshCalls++
                refreshed
            } else {
                current
            }
    }

    private fun client(
        tokens: TokenSource,
        base: String = "http://localhost:18001/",
        onUpgradeRequired: () -> Unit = {},
        clientVersion: String = CLIENT_VERSION,
        handler: MockRequestHandler
    ) = buildBackendClient(
        engine = MockEngine(handler),
        baseUrl = base,
        tokens = tokens,
        isDebug = false,
        clientVersion = clientVersion,
        onUpgradeRequired = onUpgradeRequired
    )

    // On real time: runTest's virtual clock skips ahead while MockEngine answers on another dispatcher,
    // so HttpTimeout's 45 s timer would fire before the response arrives.
    private fun apiTest(block: suspend CoroutineScope.() -> Unit): TestResult =
        runTest { withContext(Dispatchers.Default) { block() } }

    @Test
    fun sendsBearerTokenOnFirstRequest() = apiTest {
        var seen: String? = null
        client(FakeTokens("t1", "t2")) { req ->
            seen = req.headers[HttpHeaders.Authorization]
            respondOk("[]")
        }.get("spaces")
        assertEquals("Bearer t1", seen)
    }

    @Test
    fun unauthorizedRefreshesOnceAndRetries() = apiTest {
        val tokens = FakeTokens("old", "new")
        val seen = mutableListOf<String?>()
        client(tokens) { req ->
            seen += req.headers[HttpHeaders.Authorization]
            if (seen.size == 1) respondError(HttpStatusCode.Unauthorized) else respondOk("[]")
        }.get("spaces")
        assertEquals(listOf<String?>("Bearer old", "Bearer new"), seen)
        assertEquals(1, tokens.refreshCalls)
    }

    @Test
    fun secondUnauthorizedSurfacesAsError() = apiTest {
        val tokens = FakeTokens("old", "new")
        val c = client(tokens) { respondError(HttpStatusCode.Unauthorized) }
        val error = assertFailsWith<ClientRequestException> { c.get("spaces") }
        assertEquals(401, error.response.status.value)
        assertEquals(1, tokens.refreshCalls)
    }

    // The same client loads Coil's images: the ID token must never reach a host other than the API's
    @Test
    fun otherHostsGetNoToken() = apiTest {
        val seen = mutableListOf<String?>()
        client(FakeTokens("t", "t2")) { req ->
            seen += req.headers[HttpHeaders.Authorization]
            respondOk("[]")
        }.get("https://images.example.com/avatar.png")
        assertEquals(listOf<String?>(null), seen)
    }

    @Test
    fun unauthorizedFromAnotherHostSendsNoTokenAndRefreshesNothing() = apiTest {
        val tokens = FakeTokens("t", "t2")
        val seen = mutableListOf<String?>()
        val c = client(tokens) { req ->
            if (req.url.host == "images.example.com") {
                seen += req.headers[HttpHeaders.Authorization]
                respond("", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.WWWAuthenticate, "Bearer"))
            } else {
                respondOk("[]")
            }
        }
        assertFailsWith<ClientRequestException> { c.get("https://images.example.com/avatar.png") }
        assertEquals(listOf<String?>(null), seen)
        assertEquals(0, tokens.refreshCalls)
    }

    @Test
    fun unauthorizedFromAnotherHostWithoutChallengeSendsNoToken() = apiTest {
        val tokens = FakeTokens("t", "t2")
        val seen = mutableListOf<String?>()
        val c = client(tokens) { req ->
            seen += req.headers[HttpHeaders.Authorization]
            respondError(HttpStatusCode.Unauthorized)
        }
        assertFailsWith<ClientRequestException> { c.get("https://images.example.com/avatar.png") }
        assertEquals(listOf<String?>(null), seen)
        assertEquals(0, tokens.refreshCalls)
    }

    @Test
    fun apiStillGetsItsTokenAfterAnotherHostsUnauthorized() = apiTest {
        val seen = mutableListOf<String?>()
        val c = client(FakeTokens("t", "t2")) { req ->
            if (req.url.host == "images.example.com") {
                respondError(HttpStatusCode.Unauthorized)
            } else {
                seen += req.headers[HttpHeaders.Authorization]
                respondOk("[]")
            }
        }
        c.get("spaces")
        assertFailsWith<ClientRequestException> { c.get("https://images.example.com/avatar.png") }
        c.get("spaces")
        assertEquals(listOf<String?>("Bearer t", "Bearer t"), seen)
    }

    // Sign-out drops the cached token, so the next account never sends the previous one's
    @Test
    fun forgottenTokenIsReloadedForTheNextUser() = apiTest {
        val tokens = FakeTokens("userA", null)
        val seen = mutableListOf<String?>()
        val c = client(tokens) { req ->
            seen += req.headers[HttpHeaders.Authorization]
            respondOk("[]")
        }
        c.get("spaces")
        c.forgetToken()
        tokens.current = "userB"
        c.get("spaces")
        assertEquals(listOf<String?>("Bearer userA", "Bearer userB"), seen)
    }

    // Signed out: the request still goes out, just without an Authorization header
    @Test
    fun noTokenSendsNoAuthorizationHeader() = apiTest {
        var seen: String? = "unset"
        client(FakeTokens(null, null)) { req ->
            seen = req.headers[HttpHeaders.Authorization]
            respondOk("[]")
        }.get("spaces")
        assertEquals(null, seen)
    }

    // The API is host and port: another service on the same machine gets no token
    @Test
    fun sameHostOnAnotherPortGetsNoToken() = apiTest {
        val seen = mutableListOf<String?>()
        client(FakeTokens("t", "t2")) { req ->
            seen += req.headers[HttpHeaders.Authorization]
            respondOk("[]")
        }.get("http://localhost:8080/spaces")
        assertEquals(listOf<String?>(null), seen)
    }

    @Test
    fun pathsKeepTheBasePrefix() = apiTest {
        var url: Url? = null
        client(FakeTokens("t", null), base = "https://api.example.com/app/") { req ->
            url = req.url
            respondOk("[]")
        }.get("spaces/abc")
        assertEquals("/app/spaces/abc", url!!.encodedPath)
    }

    // The server compares the app's version; image hosts never learn it
    @Test
    fun sendsTheClientVersionToTheApiOnly() = apiTest {
        val seen = mutableMapOf<String, String?>()
        val c = client(FakeTokens("t", null)) { req ->
            seen[req.url.host] = req.headers[CLIENT_VERSION_HEADER]
            respondOk("[]")
        }
        c.get("spaces")
        c.get("https://images.example.com/avatar.png")
        assertEquals(mapOf("localhost" to CLIENT_VERSION, "images.example.com" to null), seen)
    }

    // The server answers 400 to a malformed version but lets a request without one through
    @Test
    fun aVersionThatIsNotMajorMinorPatchIsNotSent() = apiTest {
        listOf("1.0", "", " ", "1.0.0-beta", "01.2.3").forEach { version ->
            var seen: String? = "unset"
            client(FakeTokens("t", null), clientVersion = version) { req ->
                seen = req.headers[CLIENT_VERSION_HEADER]
                respondOk("[]")
            }.get("spaces")
            assertEquals(null, seen, "version \"$version\"")
        }
    }

    // The 426 still surfaces as an error to the caller; the callback opens the forced-update screen
    @Test
    fun upgradeRequiredCallsTheCallback() = apiTest {
        var calls = 0
        val c = client(FakeTokens("t", null), onUpgradeRequired = { calls++ }) {
            respond(
                """{"error":{"code":"CLIENT_UPGRADE_REQUIRED","message":"Update the app","minVersion":"2.0.0"}}""",
                HttpStatusCode.UpgradeRequired,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val error = assertFailsWith<ClientRequestException> { c.get("spaces") }
        assertEquals(426, error.response.status.value)
        assertEquals(1, calls)
    }

    @Test
    fun upgradeRequiredFromAnotherHostIsIgnored() = apiTest {
        var calls = 0
        val c = client(FakeTokens("t", null), onUpgradeRequired = { calls++ }) {
            respondError(HttpStatusCode.UpgradeRequired)
        }
        assertFailsWith<ClientRequestException> { c.get("https://images.example.com/avatar.png") }
        assertEquals(0, calls)
    }
}
