package com.kturker.multiplatform.core.insights.consent

import android.content.SharedPreferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// The answers live in the app's own SharedPreferences (not core/database, which a project without DataStore lacks)
class SharedPreferencesConsentStoreTest {

    private val preferences = FakeSharedPreferences()

    @Test
    fun aNewStoreHasNoAnswerAndCrashReportsOn() {
        val store = SharedPreferencesConsentStore(preferences)

        assertEquals(AnalyticsConsent.Unknown, store.analytics)
        assertTrue(store.crashReports)
    }

    @Test
    fun answersOutliveTheStore() {
        SharedPreferencesConsentStore(preferences).save(analytics = AnalyticsConsent.Granted, crashReports = false)

        val reopened = SharedPreferencesConsentStore(preferences)

        assertEquals(AnalyticsConsent.Granted, reopened.analytics)
        assertFalse(reopened.crashReports)

        reopened.save(analytics = AnalyticsConsent.Denied, crashReports = true)
        assertEquals(AnalyticsConsent.Denied, SharedPreferencesConsentStore(preferences).analytics)
        assertTrue(SharedPreferencesConsentStore(preferences).crashReports)
    }

    // A value this version does not know (a later version's answer, a damaged file) asks again
    @Test
    fun anUnknownValueAsksAgain() {
        preferences.edit().putString("analytics", "Maybe").apply()

        assertEquals(AnalyticsConsent.Unknown, SharedPreferencesConsentStore(preferences).analytics)
    }

    // Host tests have no Android Context (and so no Firebase): the answers are kept in memory
    @Test
    fun withoutAContextTheStoreIsInMemory() {
        assertIs<InMemoryConsentStore>(platformConsentStore())
    }
}

private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()

    override fun getString(
        key: String,
        defValue: String?
    ): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(
        key: String,
        defValues: Set<String>?
    ): Set<String>? = values[key] as? Set<String> ?: defValues

    override fun getInt(
        key: String,
        defValue: Int
    ): Int = values[key] as? Int ?: defValue

    override fun getLong(
        key: String,
        defValue: Long
    ): Long = values[key] as? Long ?: defValue

    override fun getFloat(
        key: String,
        defValue: Float
    ): Float = values[key] as? Float ?: defValue

    override fun getBoolean(
        key: String,
        defValue: Boolean
    ): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = key in values

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removed = mutableSetOf<String>()
        private var clear = false

        override fun putString(
            key: String,
            value: String?
        ) = apply { pending[key] = value }

        override fun putStringSet(
            key: String,
            values: Set<String>?
        ) = apply { pending[key] = values }

        override fun putInt(
            key: String,
            value: Int
        ) = apply { pending[key] = value }

        override fun putLong(
            key: String,
            value: Long
        ) = apply { pending[key] = value }

        override fun putFloat(
            key: String,
            value: Float
        ) = apply { pending[key] = value }

        override fun putBoolean(
            key: String,
            value: Boolean
        ) = apply { pending[key] = value }

        override fun remove(key: String) = apply { removed += key }

        override fun clear() = apply { clear = true }

        override fun commit(): Boolean {
            if (clear) values.clear()
            removed.forEach(values::remove)
            values.putAll(pending)
            return true
        }

        override fun apply() {
            commit()
        }
    }
}
