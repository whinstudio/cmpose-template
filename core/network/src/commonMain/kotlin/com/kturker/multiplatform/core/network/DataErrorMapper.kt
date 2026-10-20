package com.kturker.multiplatform.core.network

import com.kturker.multiplatform.core.domain.DataError
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.ContentConvertException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.cancellation.CancellationException

/**
 * The reason behind a failed request. With expectSuccess a status outside 2xx is a ResponseException;
 * Ktor's timeouts (HttpRequestTimeoutException, ConnectTimeoutException, SocketTimeoutException) and
 * the engines' connection errors are IOExceptions. Callers rethrow CancellationException first.
 */
fun Throwable.toDataError(): DataError = when (this) {
    is ResponseException -> DataError.Http(code = response.status.value)
    is IOException -> DataError.Network
    is NoTransformationFoundException, is ContentConvertException -> DataError.Serialization
    is SerializationException -> DataError.Serialization
    else -> DataError.Unknown(cause = this)
}

/**
 * [toDataError] plus the backend's error code: a [ResponseException]'s body `{"error":{"code":…}}` fills
 * [DataError.Http.apiCode]. Read as a plain JsonObject, so this module needs no core:api types.
 */
internal suspend fun Throwable.toDataErrorWithApiCode(): DataError {
    val error = toDataError()
    if (this !is ResponseException || error !is DataError.Http) return error
    return error.copy(apiCode = apiErrorCode())
}

// Null for any body that is not the backend's error shape (an HTML proxy page, another host's JSON, no body).
// expectSuccess keeps the failed response's body, so it can be read here a second time.
private suspend fun ResponseException.apiErrorCode(): String? = try {
    val body = Json.parseToJsonElement(response.bodyAsText()) as? JsonObject
    val code = (body?.get("error") as? JsonObject)?.get("code") as? JsonPrimitive
    code?.takeIf { it.isString }?.content
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
