package com.kturker.multiplatform.core.insights.consent

import com.kturker.multiplatform.core.insights.Insights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The user's answer to the analytics prompt. Unknown until they answer: a dismissed prompt asks again. */
enum class AnalyticsConsent { Unknown, Granted, Denied }

/**
 * Where the answers are kept: the platform's own key-value storage (SharedPreferences, NSUserDefaults), not
 * core/database, which a project without DataStore does not have. A new store has no analytics answer and crash
 * reports on.
 */
interface InsightsConsentStore {
    val analytics: AnalyticsConsent
    val crashReports: Boolean

    fun save(
        analytics: AnalyticsConsent,
        crashReports: Boolean
    )
}

/**
 * Analytics stays off until the user agrees; crash reports are on until the user turns them off. Firebase starts
 * with Analytics already off (the manifest's and Info.plist's FIREBASE_ANALYTICS_COLLECTION_ENABLED), and [apply]
 * turns on what the saved answers allow. Each answer is saved and reaches Firebase at once.
 */
class InsightsConsentController(private val store: InsightsConsentStore, private val insights: Insights) {

    private val analytics = MutableStateFlow(store.analytics)
    val analyticsConsent: StateFlow<AnalyticsConsent> = analytics.asStateFlow()

    private val crashReports = MutableStateFlow(store.crashReports)
    val crashReportsEnabled: StateFlow<Boolean> = crashReports.asStateFlow()

    /**
     * True until the user answers, on every launch: a dismissed prompt or a killed app leaves the answer Unknown.
     * False without the app's Firebase config, where there is no analytics to ask about.
     */
    val needsAnalyticsPrompt: Boolean
        get() = insights.isEnabled && analytics.value == AnalyticsConsent.Unknown

    // Tapping outside or back: the prompt goes for this run only, and comes back at the next launch
    private var promptDismissed = false

    private val promptPending = MutableStateFlow(false)

    /**
     * True while the prompt is to be shown now: [needsAnalyticsPrompt], and not dismissed in this run. The prompt
     * (InsightsConsentDialog) shows while it is true, and the first-launch paywall waits for it
     * (AnalyticsLaunchPrompt).
     */
    val analyticsPromptPending: StateFlow<Boolean> = promptPending.asStateFlow()

    init {
        updatePromptPending()
    }

    /**
     * Hands the saved answers to Firebase. At launch: Android when Koin starts (InsightsModule), iOS once the Swift
     * bridge is registered (registerInsightsBridge), since before that no call reaches Firebase.
     */
    fun apply() {
        insights.setAnalyticsEnabled(analytics.value == AnalyticsConsent.Granted)
        insights.setCrashReportsEnabled(crashReports.value)
        // iOS: Firebase is there only from now on, so the prompt may have become due
        updatePromptPending()
    }

    fun grantAnalytics() = answerAnalytics(AnalyticsConsent.Granted)

    fun denyAnalytics() = answerAnalytics(AnalyticsConsent.Denied)

    /** The prompt was closed without an answer: nothing is saved, and it is not shown again in this run. */
    fun dismissAnalyticsPrompt() {
        promptDismissed = true
        updatePromptPending()
    }

    fun setCrashReports(enabled: Boolean) {
        store.save(analytics = analytics.value, crashReports = enabled)
        crashReports.value = enabled
        insights.setCrashReportsEnabled(enabled)
    }

    private fun answerAnalytics(answer: AnalyticsConsent) {
        store.save(analytics = answer, crashReports = crashReports.value)
        analytics.value = answer
        insights.setAnalyticsEnabled(answer == AnalyticsConsent.Granted)
        updatePromptPending()
    }

    private fun updatePromptPending() {
        promptPending.value = needsAnalyticsPrompt && !promptDismissed
    }
}

/** Android without a Context (host tests): kept for the process only. There is no Firebase there either. */
internal class InMemoryConsentStore : InsightsConsentStore {
    override var analytics = AnalyticsConsent.Unknown
        private set
    override var crashReports = true
        private set

    override fun save(
        analytics: AnalyticsConsent,
        crashReports: Boolean
    ) {
        this.analytics = analytics
        this.crashReports = crashReports
    }
}
