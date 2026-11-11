package com.kturker.multiplatform.core.insights.consent

import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// The answers live in the app's own NSUserDefaults (not core/database, which a project without DataStore lacks)
class UserDefaultsConsentStoreTest {

    private val defaults = NSUserDefaults(suiteName = SUITE)

    // The simulator keeps the suite between runs, and removePersistentDomainForName does not reliably reach a
    // suite's cached values: each test starts and ends without the store's keys
    @BeforeTest
    @AfterTest
    fun clear() {
        KEYS.forEach(defaults::removeObjectForKey)
    }

    @Test
    fun aNewStoreHasNoAnswerAndCrashReportsOn() {
        val store = UserDefaultsConsentStore(defaults)

        assertEquals(AnalyticsConsent.Unknown, store.analytics)
        assertTrue(store.crashReports)
    }

    @Test
    fun answersOutliveTheStore() {
        UserDefaultsConsentStore(defaults).save(analytics = AnalyticsConsent.Granted, crashReports = false)

        val reopened = UserDefaultsConsentStore(defaults)

        assertEquals(AnalyticsConsent.Granted, reopened.analytics)
        assertFalse(reopened.crashReports)

        reopened.save(analytics = AnalyticsConsent.Denied, crashReports = true)
        assertEquals(AnalyticsConsent.Denied, UserDefaultsConsentStore(defaults).analytics)
        assertTrue(UserDefaultsConsentStore(defaults).crashReports)
    }

    // A value this version does not know (a later version's answer, a damaged file) asks again
    @Test
    fun anUnknownValueAsksAgain() {
        defaults.setObject("Maybe", forKey = KEYS.first())

        assertEquals(AnalyticsConsent.Unknown, UserDefaultsConsentStore(defaults).analytics)
    }

    private companion object {
        const val SUITE = "com.kturker.multiplatform.core.insights.consent.test"
        val KEYS = listOf("insights.analyticsConsent", "insights.crashReports")
    }
}
