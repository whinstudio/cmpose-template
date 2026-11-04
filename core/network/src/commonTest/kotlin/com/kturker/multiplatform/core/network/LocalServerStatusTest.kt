package com.kturker.multiplatform.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val API = "http://localhost:18001/spaces"

class LocalServerStatusTest {

    private val noTokens = object : TokenSource {
        override suspend fun token(forceRefresh: Boolean): String? = null
    }

    // Not buildBackendClient: in debug it also installs the network monitor, whose Koin a host test does not have.
    // Same order as there: the watch first, then HttpTimeout.
    private fun client(status: LocalServerStatus, timeoutMillis: Long = 45_000, handler: MockRequestHandler) =
        HttpClient(MockEngine(handler)) {
            expectSuccess = true
            installLocalServerWatch(status) { it.host == "localhost" && it.port == 18_001 }
            install(HttpTimeout) { requestTimeoutMillis = timeoutMillis }
        }

    // On real time: runTest's virtual clock skips ahead while MockEngine answers on another dispatcher,
    // so HttpTimeout's 45 s timer would fire before the response arrives.
    private fun apiTest(block: suspend CoroutineScope.() -> Unit): TestResult =
        runTest { withContext(Dispatchers.Default) { block() } }

    @Test
    fun aConnectionErrorShowsTheHint() = apiTest {
        val status = LocalServerStatus()
        runCatching { client(status) { throw IOException("refused") }.get(API) }
        assertTrue(status.isUnreachable.value)
    }

    @Test
    fun aTimeoutShowsTheHint() = apiTest {
        val connect = LocalServerStatus()
        runCatching { client(connect) { throw ConnectTimeoutException("connect") }.get(API) }
        assertTrue(connect.isUnreachable.value, "connect timeout")

        val request = LocalServerStatus()
        runCatching { client(request) { throw HttpRequestTimeoutException(API, 1) }.get(API) }
        assertTrue(request.isUnreachable.value, "request timeout")
    }

    // HttpTimeout cancels the call with its exception as the cause; inside Send the hook sees the cancellation
    @Test
    fun aTimeoutInsideACancellationShowsTheHint() = apiTest {
        val status = LocalServerStatus()
        runCatching {
            client(status) {
                throw CancellationException("timed out", HttpRequestTimeoutException(API, 1))
            }.get(API)
        }
        assertTrue(status.isUnreachable.value)
    }

    // HttpTimeout's own timer, not one thrown by the engine
    @Test
    fun httpTimeoutsOwnTimerShowsTheHint() = apiTest {
        val status = LocalServerStatus()
        val c = client(status, timeoutMillis = 50) {
            delay(10_000)
            respondOk("[]")
        }
        val error = runCatching { c.get(API) }.exceptionOrNull()
        assertIs<HttpRequestTimeoutException>(error)
        assertTrue(status.isUnreachable.value)
    }

    // A 401 is an answer too
    @Test
    fun anyResponseHidesTheHint() = apiTest {
        val status = LocalServerStatus()
        var answer: HttpStatusCode? = null
        val c = client(status) {
            val code = answer ?: throw IOException("refused")
            if (code == HttpStatusCode.OK) respondOk("[]") else respondError(code)
        }
        listOf(HttpStatusCode.OK, HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError).forEach { code ->
            answer = null
            runCatching { c.get(API) }
            assertTrue(status.isUnreachable.value, "before $code")
            answer = code
            runCatching { c.get(API) }
            assertFalse(status.isUnreachable.value, "after $code")
        }
    }

    @Test
    fun anHttpErrorNeverShowsTheHint() = apiTest {
        val status = LocalServerStatus()
        listOf(HttpStatusCode.InternalServerError, HttpStatusCode.NotFound).forEach { code ->
            runCatching { client(status) { respondError(code) }.get(API) }
            assertFalse(status.isUnreachable.value, "$code")
        }
    }

    @Test
    fun aReleaseBuildNeverShowsTheHint() = apiTest {
        val status = LocalServerStatus()
        runCatching {
            buildBackendClient(
                engine = MockEngine { throw IOException("refused") },
                baseUrl = "http://localhost:18001/",
                tokens = noTokens,
                isDebug = false,
                clientVersion = "1.2.3",
                onUpgradeRequired = {},
                localServerStatus = status
            ).get(API)
        }
        assertFalse(status.isUnreachable.value)
    }

    // The same client loads Coil's images: another host says nothing about the local server
    @Test
    fun otherHostsChangeNothing() = apiTest {
        val status = LocalServerStatus()
        val c = client(status) { req ->
            if (req.url.host == "images.example.com") respondOk("png") else throw IOException("refused")
        }
        runCatching { c.get(API) }
        c.get("https://images.example.com/a.png")
        assertTrue(status.isUnreachable.value, "an image answered")

        val fresh = LocalServerStatus()
        runCatching { client(fresh) { throw IOException("refused") }.get("https://images.example.com/a.png") }
        assertFalse(fresh.isUnreachable.value, "an image failed")
    }

    @Test
    fun aCancelledRequestIsNotAConnectionError() = apiTest {
        val status = LocalServerStatus()
        runCatching { client(status) { throw CancellationException("left") }.get(API) }
        assertFalse(status.isUnreachable.value)
    }

    @Test
    fun localServerCommandsAreExact() {
        assertEquals("./gradlew :server:runLocal", LOCAL_SERVER_RUN_COMMAND)
        if (localServerUsesAdbReverse) {
            assertEquals("adb reverse tcp:18001 tcp:18001", localServerAdbReverseCommand)
        } else {
            assertNull(localServerAdbReverseCommand)
        }
    }
}
