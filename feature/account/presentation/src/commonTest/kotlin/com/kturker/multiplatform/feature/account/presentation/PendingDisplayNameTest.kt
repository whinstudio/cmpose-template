package com.kturker.multiplatform.feature.account.presentation

import com.kturker.multiplatform.feature.account.presentation.data.DataStorePendingDisplayName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PendingDisplayNameTest {

    private val dataStore = InMemoryPreferences()

    @Test
    fun aPutNameIsReadUntilCleared() = runTest {
        val store = DataStorePendingDisplayName(dataStore)

        store.put("u1", "Ada")

        assertEquals("Ada", store.get("u1"))
        assertEquals("Ada", store.get("u1"))
        store.clear("u1")
        assertNull(store.get("u1"))
    }

    @Test
    fun nothingPutReadsNothing() = runTest {
        assertNull(DataStorePendingDisplayName(dataStore).get("u1"))
    }

    // Two accounts on one device: each user's name is their own
    @Test
    fun namesAreKeptPerUser() = runTest {
        val store = DataStorePendingDisplayName(dataStore)

        store.put("u1", "Ada")
        store.put("u2", "Grace")
        store.clear("u2")

        assertEquals("Ada", store.get("u1"))
        assertNull(store.get("u2"))
    }

    @Test
    fun aNewerNameReplacesTheOlderOne() = runTest {
        val store = DataStorePendingDisplayName(dataStore)

        store.put("u1", "Ada")
        store.put("u1", "Ada L.")

        assertEquals("Ada L.", store.get("u1"))
    }

    // Stored, not held in memory: the app may be restarted before the next sign-in
    @Test
    fun theNameOutlivesTheStoreInstance() = runTest {
        DataStorePendingDisplayName(dataStore).put("u1", "Ada")

        assertEquals("Ada", DataStorePendingDisplayName(dataStore).get("u1"))
    }
}
