package com.kturker.multiplatform.core.network

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.domain.DataError

private val log = Logger.withTag("Network")

// BaseRepository.request is inline, so its logging lives here. Only the status code or the error's
// and exception's types are written: never the URL, headers, body or message, which can carry user
// data or tokens.
@PublishedApi
internal suspend fun Exception.toLoggedDataError(): DataError {
    val error = toDataErrorWithApiCode()
    log.w { "Request failed: ${error.describe(exception = this)}" }
    return error
}

// Fixed labels, not class names: R8 renames the app's classes in release builds. The exception's
// simpleName stays readable there because R8 keeps platform and library exception names.
private fun DataError.describe(exception: Exception): String = when (this) {
    is DataError.Http -> "HTTP $code"
    DataError.Network -> "Network (${exception::class.simpleName})"
    DataError.Serialization -> "Serialization (${exception::class.simpleName})"
    is DataError.Unknown -> "Unknown (${exception::class.simpleName})"
}
