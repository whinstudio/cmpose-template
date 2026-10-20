package com.kturker.multiplatform.core.network

import com.kturker.multiplatform.core.domain.RestResult
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

open class BaseRepository {
    // CancellationException is rethrown: the caller was cancelled (the screen was left, a newer
    // search started) and has to stop here instead of receiving an error result. A failed status keeps the
    // backend's error code from the body (DataError.Http.apiCode).
    suspend inline fun <reified T : Any> request(
        crossinline call: suspend () -> HttpResponse
    ): RestResult<T> = withContext(context = Dispatchers.IO) {
        try {
            call.invoke().asRestResult()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            RestResult.Error(e.toLoggedDataError())
        }
    }
}
