package com.kturker.multiplatform.core.insights

/**
 * Crash reports (Crashlytics) and analytics (Firebase Analytics). Without the app's Firebase config
 * (`google-services.json`, `GoogleService-Info.plist`) [isEnabled] is false and every call does nothing.
 * Never pass tokens, request or response bodies, headers, emails or names.
 */
interface Insights {
    val isEnabled: Boolean

    fun logEvent(
        name: String,
        params: Map<String, Any> = emptyMap()
    )

    /** The backend's user id only, never an email; null clears it. */
    fun setUserId(id: String?)

    fun recordNonFatal(
        throwable: Throwable,
        message: String? = null
    )

    /** A breadcrumb: sent with the next crash or non-fatal. */
    fun log(message: String)

    fun setCollectionEnabled(enabled: Boolean)
}

/** No Firebase config: off, and every call returns at once. */
internal object NoOpInsights : Insights {
    override val isEnabled = false

    override fun logEvent(
        name: String,
        params: Map<String, Any>
    ) = Unit

    override fun setUserId(id: String?) = Unit

    override fun recordNonFatal(
        throwable: Throwable,
        message: String?
    ) = Unit

    override fun log(message: String) = Unit

    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
