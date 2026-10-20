package com.kturker.multiplatform.core.network

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.TestLogWriter
import co.touchlab.kermit.platformLogWriter
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalKermitApi::class)
class BaseRepositoryTest {

    private val repository = BaseRepository()

    private val writer = TestLogWriter(loggable = Severity.Verbose)

    @BeforeTest
    fun setUp() {
        Logger.setLogWriters(writer)
        Logger.setMinSeverity(Severity.Verbose)
    }

    @AfterTest
    fun tearDown() {
        Logger.setLogWriters(platformLogWriter())
    }

    @Test
    fun aFailedCallBecomesAnErrorWithItsReason() = runTest {
        val result = repository.request<String> { throw responseException(HttpStatusCode.Forbidden) }

        assertEquals(RestResult.Error(DataError.Http(code = 403)), result)
    }

    // The backend's {"error":{"code":…}} body: the code tells a ViewModel which failure it is
    @Test
    fun aFailedCallKeepsTheServersErrorCode() = runTest {
        val client = failingClient(
            status = HttpStatusCode.Conflict,
            body = """{"error":{"code":"INVITE_USED","message":"Already used","requestId":"r1"}}"""
        )

        val result = repository.request<String> { client.get("invites/abc") }

        assertEquals(RestResult.Error(DataError.Http(code = 409, apiCode = "INVITE_USED")), result)
    }

    @Test
    fun aNonJsonErrorBodyLeavesTheCodeNull() = runTest {
        val client = failingClient(status = HttpStatusCode.BadGateway, body = "<html>Bad gateway</html>")

        val result = repository.request<String> { client.get("spaces") }

        assertEquals(RestResult.Error(DataError.Http(code = 502)), result)
    }

    @Test
    fun aConnectionFailureBecomesANetworkError() = runTest {
        val result = repository.request<String> { throw IOException("offline") }

        assertEquals(RestResult.Error(DataError.Network), result)
    }

    // The caller was cancelled (screen left, newer search): it must stop, not receive an error
    @Test
    fun cancellationIsRethrownNotTurnedIntoAnError() = runTest {
        assertFailsWith<CancellationException> {
            repository.request<String> { throw CancellationException("screen left") }
        }
    }

    // Only the status: the URL and the body (ResponseException's message carries both) stay out of the log
    @Test
    fun aFailedCallIsAWarningWithItsStatusOnly() = runTest {
        repository.request<String> {
            throw ResponseException(
                response = FakeHttpResponse(HttpStatusCode.Forbidden),
                cachedResponseText = "{\"token\":\"secret\"}"
            )
        }

        val entry = writer.logs.single()
        assertEquals(Severity.Warn, entry.severity)
        assertEquals("Network", entry.tag)
        assertEquals("Request failed: HTTP 403", entry.message)
        assertNull(entry.throwable)
    }

    // Fixed labels, so R8's renamed classes never reach a release log
    @Test
    fun everyOtherFailureIsAWarningWithItsKindAndExceptionType() = runTest {
        repository.request<String> { throw IOException("Unable to resolve host api.github.com") }
        repository.request<String> { throw SerializationException("unexpected token") }
        repository.request<String> { throw IllegalStateException("unexpected") }

        assertEquals(
            listOf(
                "Request failed: Network (IOException)",
                "Request failed: Serialization (SerializationException)",
                "Request failed: Unknown (IllegalStateException)"
            ),
            writer.logs.map { it.message }
        )
        assertEquals(listOf(Severity.Warn), writer.logs.map { it.severity }.distinct())
        assertEquals(listOf("Network"), writer.logs.map { it.tag }.distinct())
    }

    @Test
    fun aCancelledCallIsNotLogged() = runTest {
        assertFailsWith<CancellationException> {
            repository.request<String> { throw CancellationException("screen left") }
        }

        writer.assertCount(0)
    }

    // expectSuccess, like the app's clients: a status outside 2xx throws a ResponseException
    private fun failingClient(status: HttpStatusCode, body: String) = HttpClient(
        MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
    ) {
        expectSuccess = true
    }
}
