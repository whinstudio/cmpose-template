package com.kturker.multiplatform.core.insights

import kotlin.experimental.ExperimentalNativeApi

/**
 * iOSApp.init() starts Koin, which creates Insights for the log writer, before it registers the Swift bridge:
 * the bridge is looked up on every call, and calls do nothing until it is there and Firebase is configured.
 */
internal class BridgeInsights(private val bridge: () -> IosInsightsBridge?) : Insights {

    override val isEnabled: Boolean get() = bridge()?.isConfigured == true

    override fun logEvent(
        name: String,
        params: Map<String, Any>
    ) {
        configured()?.logEvent(name, params)
    }

    override fun setUserId(id: String?) {
        configured()?.setUserId(id)
    }

    // getStackTrace() is Kotlin/Native's own API
    @OptIn(ExperimentalNativeApi::class)
    override fun recordNonFatal(
        throwable: Throwable,
        message: String?
    ) {
        val bridge = configured() ?: return
        val description = listOfNotNull(message, throwable.message).joinToString(": ").ifEmpty { null }
        bridge.recordNonFatal(
            name = throwable::class.simpleName ?: "Throwable",
            message = description,
            stack = throwable.getStackTrace().toList()
        )
    }

    override fun log(message: String) {
        configured()?.log(message)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        configured()?.setCollectionEnabled(enabled)
    }

    private fun configured(): IosInsightsBridge? = bridge()?.takeIf { it.isConfigured }
}

internal actual fun platformInsights(): Insights =
    BridgeInsights(bridge = IosInsights::bridgeOrNull)
