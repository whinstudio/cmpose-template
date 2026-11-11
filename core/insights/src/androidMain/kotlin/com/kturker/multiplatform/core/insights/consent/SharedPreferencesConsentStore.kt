package com.kturker.multiplatform.core.insights.consent

import android.content.Context
import android.content.SharedPreferences
import org.koin.mp.KoinPlatform

internal class SharedPreferencesConsentStore(private val preferences: SharedPreferences) : InsightsConsentStore {

    // A value this version does not know asks again
    override val analytics: AnalyticsConsent
        get() = AnalyticsConsent.entries.firstOrNull { it.name == preferences.getString(KEY_ANALYTICS, null) }
            ?: AnalyticsConsent.Unknown

    override val crashReports: Boolean
        get() = preferences.getBoolean(KEY_CRASH_REPORTS, true)

    override fun save(
        analytics: AnalyticsConsent,
        crashReports: Boolean
    ) = preferences.edit()
        .putString(KEY_ANALYTICS, analytics.name)
        .putBoolean(KEY_CRASH_REPORTS, crashReports)
        .apply()

    private companion object {
        const val KEY_ANALYTICS = "analytics"
        const val KEY_CRASH_REPORTS = "crash_reports"
    }
}

internal actual fun platformConsentStore(): InsightsConsentStore {
    val context = KoinPlatform.getKoinOrNull()?.getOrNull<Context>() ?: return InMemoryConsentStore()
    return SharedPreferencesConsentStore(context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE))
}

private const val PREFERENCES = "insights_consent"
