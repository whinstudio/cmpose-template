package com.kturker.multiplatform.core.domain

/** Why a request failed. [RestResult.Error] carries it to the ViewModel, which picks the message. */
sealed interface DataError {

    /** No connection, a timeout or an unknown host. */
    data object Network : DataError

    /**
     * The server answered with [code], a status outside 2xx. [apiCode] is the backend's own error code
     * (`{"error":{"code":…}}`, core:api ErrorCodes), null when the body carries none.
     */
    data class Http(val code: Int, val apiCode: String? = null) : DataError

    /** The response body could not be read into the expected type. */
    data object Serialization : DataError

    /** Any other failure; [cause] is kept for logging. */
    data class Unknown(val cause: Throwable) : DataError
}
