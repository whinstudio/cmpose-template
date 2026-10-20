package com.kturker.multiplatform.core.network

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Koin name of the backend's [HttpClient]; the unnamed one stays the public API's client (NetworkModule). */
const val BACKEND_CLIENT = "backend"

/** The app's `MAJOR.MINOR.PATCH`; the backend answers 426 `CLIENT_UPGRADE_REQUIRED` below its minimum version. */
const val CLIENT_VERSION_HEADER = "X-Client-Version"

private const val TIME_OUT = 45_000L

// The server's format: three numbers without leading zeros. It answers 400 to any other value, while a request
// without the header passes, so a version outside this form is not sent at all.
private val VERSION_FORMAT = Regex("""^(0|[1-9]\d{0,8})\.(0|[1-9]\d{0,8})\.(0|[1-9]\d{0,8})$""")

private val log = Logger.withTag("Network")

/**
 * Builds the backend client: base URL, JSON, timeouts, logging, the signed-in user's bearer token and the app's
 * version ([clientVersion], sent as [CLIENT_VERSION_HEADER] only when it is `MAJOR.MINOR.PATCH`; otherwise a warning
 * is logged once and no header goes out). The backend's 426 calls [onUpgradeRequired] (the forced-update screen) and
 * still fails the request. Provided under [BACKEND_CLIENT] (core:auth), so it never replaces the app's unnamed
 * HttpClient.
 */
fun buildBackendClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokens: TokenSource,
    isDebug: Boolean,
    clientVersion: String,
    onUpgradeRequired: () -> Unit
): HttpClient {
    val api = Url(baseUrl)
    val versionHeader = clientVersion.takeIf { VERSION_FORMAT.matches(it) }
    if (versionHeader == null) {
        log.w { "App version \"$clientVersion\" is not MAJOR.MINOR.PATCH: $CLIENT_VERSION_HEADER is not sent" }
    }

    // The token goes only to the backend (same host and port): this client also loads Coil's images from any host
    fun Url.isApi(): Boolean = host == api.host && port == api.port

    return HttpClient(engine) {
        expectSuccess = true

        defaultRequest {
            url(baseUrl)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = TIME_OUT
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                }
            )
        }

        // Every backend request carries the ID token; a 401 refreshes it once and retries, a second 401 surfaces.
        // Another host's 401 is a plain error: Auth would otherwise retry it with the token attached.
        install(Auth) {
            reAuthorizeOnResponse { response ->
                response.status == HttpStatusCode.Unauthorized && response.request.url.isApi()
            }
            bearer {
                loadTokens { tokens.token(forceRefresh = false)?.let { BearerTokens(it, null) } }
                refreshTokens { tokens.token(forceRefresh = true)?.let { BearerTokens(it, null) } }
                sendWithoutRequest { request -> request.url.build().isApi() }
            }
        }

        // Like the token, the version goes to the backend only. The backend's 426 (CLIENT_UPGRADE_REQUIRED) means
        // the app is below the server's minimum version; another host's 426 says nothing about this app. Seen on
        // receipt, before expectSuccess turns the status into a ClientRequestException.
        install(
            createClientPlugin("ClientVersion") {
                onRequest { request, _ ->
                    if (versionHeader != null && request.url.build().isApi()) {
                        request.headers.append(CLIENT_VERSION_HEADER, versionHeader)
                    }
                }
                onResponse { response ->
                    if (response.status == HttpStatusCode.UpgradeRequired && response.request.url.isApi()) {
                        onUpgradeRequired()
                    }
                }
            }
        )

        installRequestLogging(isDebug = isDebug)

        // Debug builds only, like the request log: the monitor records each call (Authorization header masked)
        if (isDebug) installNetworkMonitor()
    }
}

/** Drops the cached bearer token; call on sign-out and account deletion. */
fun HttpClient.forgetToken() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
