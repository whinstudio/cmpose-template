package com.kturker.multiplatform.core.network

import com.kturker.multiplatform.core.domain.DataError
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.cancellation.CancellationException

/** Starts the local server; shown to the developer when the debug build cannot reach it. */
const val LOCAL_SERVER_RUN_COMMAND = "./gradlew :server:runLocal"

/** The tunnel to the Mac's local server, where the device needs one ([localServerUsesAdbReverse]); else `null`. */
val localServerAdbReverseCommand: String? =
    if (localServerUsesAdbReverse) {
        "adb reverse tcp:${BackendConfig.LOCAL_SERVER_PORT} tcp:${BackendConfig.LOCAL_SERVER_PORT}"
    } else {
        null
    }

/**
 * Whether the backend client's last request to the local server failed to connect. Debug builds only: a release
 * build never installs the watch, so [isUnreachable] stays `false`. Only requests to the backend API (its host and
 * port) count, never Coil's images from other hosts. A connection error (an exception that maps to
 * [DataError.Network]: refused, connect, socket or request timeout) sets it; any HTTP response, error statuses
 * included, clears it. A cancelled request changes nothing. LocalServerBanner (shared) shows it.
 */
class LocalServerStatus {
    private val unreachable = MutableStateFlow(false)

    val isUnreachable: StateFlow<Boolean> = unreachable.asStateFlow()

    internal fun markReachable() {
        unreachable.value = false
    }

    internal fun markUnreachable() {
        unreachable.value = true
    }
}

/**
 * Watches every send. Installed first, so it wraps HttpTimeout and Auth: it sees the response that follows Auth's 401
 * retry, as the caller does. HttpTimeout cancels the call with its exception as the cause, and only HttpCallValidator
 * (outside Send) unwraps it for the caller, so the cause is read here too; a cancellation without one (the caller
 * left) changes nothing.
 */
internal fun HttpClientConfig<*>.installLocalServerWatch(
    status: LocalServerStatus,
    isApi: (Url) -> Boolean
) {
    install(
        createClientPlugin("LocalServerWatch") {
            on(Send) { request ->
                val api = isApi(request.url.build())
                try {
                    proceed(request).also { if (api) status.markReachable() }
                } catch (e: Throwable) {
                    if (api) status.record(e)
                    throw e
                }
            }
        }
    )
}

private fun LocalServerStatus.record(failure: Throwable) {
    when (failure.withoutCancellation()?.toDataError()) {
        DataError.Network -> markUnreachable()

        // expectSuccess may turn the status into an exception inside Send: the server still answered
        is DataError.Http -> markReachable()

        else -> Unit
    }
}

// The first cause that is not a cancellation; null for a plain cancellation
private fun Throwable.withoutCancellation(): Throwable? {
    var failure: Throwable? = this
    while (failure is CancellationException) failure = failure.cause
    return failure
}
