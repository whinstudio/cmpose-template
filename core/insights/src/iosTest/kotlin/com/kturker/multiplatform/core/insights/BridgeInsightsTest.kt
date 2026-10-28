package com.kturker.multiplatform.core.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// iOSApp.init() starts Koin (which creates Insights) before it registers the Swift bridge: insights look the
// bridge up on every call, so they turn on once it is there
class BridgeInsightsTest {

    private var bridge: FakeIosInsightsBridge? = null
    private val insights = BridgeInsights { bridge }

    @Test
    fun disabledBeforeRegistration() {
        assertFalse(insights.isEnabled)

        insights.log("lost")
        insights.recordNonFatal(IllegalStateException("x"))
    }

    @Test
    fun forwardsAfterRegistration() {
        insights.log("before")
        val registered = FakeIosInsightsBridge().also { bridge = it }

        insights.log("after")
        insights.logEvent("screen_view", mapOf("screen" to "home"))
        insights.setUserId("u1")
        insights.setCollectionEnabled(false)

        assertTrue(insights.isEnabled)
        assertEquals(
            listOf("log:after", "event:screen_view:{screen=home}", "user:u1", "collection:false"),
            registered.calls
        )
    }

    @Test
    fun aNonFatalCarriesItsTypeMessageAndStack() {
        val registered = FakeIosInsightsBridge().also { bridge = it }

        insights.recordNonFatal(IllegalStateException("x"), "[Net] boom")

        val nonFatal = registered.nonFatals.single()
        assertEquals("IllegalStateException", nonFatal.first)
        assertEquals("[Net] boom: x", nonFatal.second)
        assertTrue(nonFatal.third.isNotEmpty())
    }

    @Test
    fun aBridgeWithoutFirebaseStaysOff() {
        val registered = FakeIosInsightsBridge(isConfigured = false).also { bridge = it }

        insights.log("lost")

        assertFalse(insights.isEnabled)
        assertTrue(registered.calls.isEmpty())
    }
}

internal class FakeIosInsightsBridge(override val isConfigured: Boolean = true) : IosInsightsBridge {
    val calls = mutableListOf<String>()
    val nonFatals = mutableListOf<Triple<String, String?, List<String>>>()

    override fun logEvent(
        name: String,
        params: Map<String, Any>
    ) {
        calls += "event:$name:$params"
    }

    override fun setUserId(id: String?) {
        calls += "user:$id"
    }

    override fun recordNonFatal(
        name: String,
        message: String?,
        stack: List<String>
    ) {
        nonFatals += Triple(name, message, stack)
    }

    override fun log(message: String) {
        calls += "log:$message"
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        calls += "collection:$enabled"
    }
}
