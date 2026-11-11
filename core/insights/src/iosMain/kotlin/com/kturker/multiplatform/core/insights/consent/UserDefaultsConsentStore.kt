package com.kturker.multiplatform.core.insights.consent

import platform.Foundation.NSUserDefaults

// The app's standard defaults (privacy manifest: UserDefaults, CA92.1), under keys of its own
internal class UserDefaultsConsentStore(private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) :
    InsightsConsentStore {

    // A value this version does not know asks again
    override val analytics: AnalyticsConsent
        get() = AnalyticsConsent.entries.firstOrNull { it.name == defaults.stringForKey(KEY_ANALYTICS) }
            ?: AnalyticsConsent.Unknown

    // boolForKey reads a missing key as false: crash reports are on until the user turns them off
    override val crashReports: Boolean
        get() = defaults.objectForKey(KEY_CRASH_REPORTS) == null || defaults.boolForKey(KEY_CRASH_REPORTS)

    override fun save(
        analytics: AnalyticsConsent,
        crashReports: Boolean
    ) {
        defaults.setObject(analytics.name, forKey = KEY_ANALYTICS)
        defaults.setBool(crashReports, forKey = KEY_CRASH_REPORTS)
    }

    private companion object {
        const val KEY_ANALYTICS = "insights.analyticsConsent"
        const val KEY_CRASH_REPORTS = "insights.crashReports"
    }
}

internal actual fun platformConsentStore(): InsightsConsentStore = UserDefaultsConsentStore()
