package com.kturker.multiplatform.core.insights.consent

import com.kturker.multiplatform.core.insights.Insights
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Analytics stays off until the user agrees, and an unanswered prompt comes back on the next launch; crash reports
// are on until the user turns them off. Each answer is saved and reaches Firebase at once.
class InsightsConsentControllerTest {

    private val store = InMemoryConsentStore()
    private val insights = SwitchRecordingInsights()

    @Test
    fun unknownConsentKeepsAnalyticsOffAndAsksAgain() {
        val controller = InsightsConsentController(store, insights)

        controller.apply()

        assertEquals(listOf(false), insights.analytics)
        assertTrue(controller.needsAnalyticsPrompt)
        assertEquals(AnalyticsConsent.Unknown, controller.analyticsConsent.value)

        // The prompt was dismissed or the app was killed: nothing was saved, so the next launch asks again
        val nextLaunch = InsightsConsentController(store, insights)
        nextLaunch.apply()

        assertEquals(listOf(false, false), insights.analytics)
        assertTrue(nextLaunch.needsAnalyticsPrompt)
    }

    @Test
    fun grantedTurnsAnalyticsOnAtStart() {
        store.saveAnalytics(AnalyticsConsent.Granted)
        val controller = InsightsConsentController(store, insights)

        controller.apply()

        assertEquals(listOf(true), insights.analytics)
        assertFalse(controller.needsAnalyticsPrompt)
        assertEquals(AnalyticsConsent.Granted, controller.analyticsConsent.value)
    }

    @Test
    fun deniedKeepsAnalyticsOffAndDoesNotAsk() {
        store.saveAnalytics(AnalyticsConsent.Denied)
        val controller = InsightsConsentController(store, insights)

        controller.apply()

        assertEquals(listOf(false), insights.analytics)
        assertFalse(controller.needsAnalyticsPrompt)
        assertEquals(AnalyticsConsent.Denied, controller.analyticsConsent.value)
    }

    @Test
    fun crashReportsAreOnByDefault() {
        val controller = InsightsConsentController(store, insights)

        controller.apply()

        assertEquals(listOf(true), insights.crashReports)
        assertTrue(controller.crashReportsEnabled.value)
    }

    @Test
    fun crashReportsFollowTheSwitch() {
        val controller = InsightsConsentController(store, insights)
        controller.apply()

        controller.setCrashReports(false)

        assertEquals(listOf(true, false), insights.crashReports)
        assertFalse(controller.crashReportsEnabled.value)
        assertFalse(store.crashReports)

        // The switch outlives the process
        val nextLaunch = InsightsConsentController(store, insights)
        nextLaunch.apply()
        assertEquals(listOf(true, false, false), insights.crashReports)
        assertFalse(nextLaunch.crashReportsEnabled.value)

        nextLaunch.setCrashReports(true)
        assertEquals(listOf(true, false, false, true), insights.crashReports)
        assertTrue(store.crashReports)
    }

    @Test
    fun grantingSavesAndAppliesAtOnce() {
        val controller = InsightsConsentController(store, insights)
        controller.apply()

        controller.grantAnalytics()

        assertEquals(AnalyticsConsent.Granted, store.analytics)
        assertEquals(listOf(false, true), insights.analytics)
        assertEquals(AnalyticsConsent.Granted, controller.analyticsConsent.value)
        assertFalse(controller.needsAnalyticsPrompt)

        controller.denyAnalytics()

        assertEquals(AnalyticsConsent.Denied, store.analytics)
        assertEquals(listOf(false, true, false), insights.analytics)
        assertEquals(AnalyticsConsent.Denied, controller.analyticsConsent.value)
    }

    @Test
    fun answeringAnalyticsKeepsTheCrashReportSwitch() {
        val controller = InsightsConsentController(store, insights)
        controller.setCrashReports(false)

        controller.grantAnalytics()

        assertFalse(store.crashReports)
        assertEquals(listOf(false), insights.crashReports)
    }

    // Two quick changes (the crash switch, then the prompt's answer) each write their own key: neither can put back
    // the other's older value
    @Test
    fun aCrashSwitchThenAnAnswerKeepsBoth() {
        val keyStore = KeyRecordingConsentStore()
        val controller = InsightsConsentController(keyStore, insights)

        controller.setCrashReports(false)
        controller.grantAnalytics()

        assertEquals(AnalyticsConsent.Granted, keyStore.analytics)
        assertFalse(keyStore.crashReports)
        assertEquals(listOf(KEY_CRASH_REPORTS, KEY_ANALYTICS), keyStore.writes)
    }

    @Test
    fun thePromptIsPendingOnlyWhileUnansweredAndNotDismissed() {
        assertTrue(InsightsConsentController(store, insights).analyticsPromptPending.value)

        val dismissed = InsightsConsentController(store, insights)
        dismissed.dismissAnalyticsPrompt()
        assertFalse(dismissed.analyticsPromptPending.value)

        store.saveAnalytics(AnalyticsConsent.Granted)
        assertFalse(InsightsConsentController(store, insights).analyticsPromptPending.value)
    }

    // Without the app's Firebase config there is no analytics to ask about
    @Test
    fun withoutFirebaseThereIsNothingToAsk() {
        val controller = InsightsConsentController(store, SwitchRecordingInsights(isEnabled = false))

        controller.apply()

        assertFalse(controller.needsAnalyticsPrompt)
        assertFalse(controller.analyticsPromptPending.value)
    }

    // The first-launch paywall waits while the prompt is pending (AnalyticsLaunchPrompt)
    @Test
    fun theUnansweredPromptIsPending() {
        val controller = InsightsConsentController(store, insights)

        assertTrue(controller.analyticsPromptPending.value)
    }

    @Test
    fun allowingEndsThePrompt() {
        val controller = InsightsConsentController(store, insights)

        controller.grantAnalytics()

        assertFalse(controller.analyticsPromptPending.value)
    }

    @Test
    fun denyingEndsThePrompt() {
        val controller = InsightsConsentController(store, insights)

        controller.denyAnalytics()

        assertFalse(controller.analyticsPromptPending.value)
    }

    // Tapping outside or back: no answer is saved, the prompt goes for this run and comes back at the next launch
    @Test
    fun dismissingEndsThePromptForThisRunOnly() {
        val controller = InsightsConsentController(store, insights)

        controller.dismissAnalyticsPrompt()

        assertFalse(controller.analyticsPromptPending.value)
        assertTrue(controller.needsAnalyticsPrompt)
        assertEquals(AnalyticsConsent.Unknown, store.analytics)
        // apply() again (iOS, once the Swift bridge is registered) does not bring it back in this run
        controller.apply()
        assertFalse(controller.analyticsPromptPending.value)

        val nextLaunch = InsightsConsentController(store, insights)
        assertTrue(nextLaunch.analyticsPromptPending.value)
    }

    @Test
    fun anEarlierAnswerIsNoPrompt() {
        store.saveAnalytics(AnalyticsConsent.Denied)

        assertFalse(InsightsConsentController(store, insights).analyticsPromptPending.value)
    }

    // iOS: Firebase reaches Kotlin only once the Swift bridge is registered, which then calls apply() again
    @Test
    fun thePromptBecomesDueOnceFirebaseIsThere() {
        val lateInsights = SwitchRecordingInsights(isEnabled = false)
        val controller = InsightsConsentController(store, lateInsights)
        assertFalse(controller.analyticsPromptPending.value)

        lateInsights.isEnabled = true
        controller.apply()

        assertTrue(controller.analyticsPromptPending.value)
    }
}

private const val KEY_ANALYTICS = "analytics"
private const val KEY_CRASH_REPORTS = "crashReports"

// Records which key each save wrote
private class KeyRecordingConsentStore : InsightsConsentStore {
    override var analytics = AnalyticsConsent.Unknown
        private set
    override var crashReports = true
        private set
    val writes = mutableListOf<String>()

    override fun saveAnalytics(answer: AnalyticsConsent) {
        analytics = answer
        writes += KEY_ANALYTICS
    }

    override fun saveCrashReports(enabled: Boolean) {
        crashReports = enabled
        writes += KEY_CRASH_REPORTS
    }
}

private class SwitchRecordingInsights(override var isEnabled: Boolean = true) : Insights {
    val analytics = mutableListOf<Boolean>()
    val crashReports = mutableListOf<Boolean>()

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

    override fun setAnalyticsEnabled(enabled: Boolean) {
        analytics += enabled
    }

    override fun setCrashReportsEnabled(enabled: Boolean) {
        crashReports += enabled
    }
}
