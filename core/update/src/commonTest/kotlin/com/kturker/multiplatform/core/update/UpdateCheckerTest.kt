package com.kturker.multiplatform.core.update

import com.kturker.multiplatform.core.about.AppVersion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private const val NOW = 1_800_000_000_000L
private const val ONE_HOUR = 60L * 60 * 1000
private const val LAST_CHECK = "update.lastCheck"
private const val LAST_REQUIRED = "update.lastRequired"

private val offline: Result<Nothing> = Result.failure(IllegalStateException("No answer"))
private val oldVersion = AppVersion(name = "1.0.0", code = "1")

class UpdateCheckerTest {

    private val store = FakeUpdateCheckStore()

    private fun checker(
        minVersions: Result<MinVersions?> = Result.success(null),
        storeRelease: Result<StoreRelease?> = Result.success(null)
    ) = UpdateChecker(
        store = store,
        minVersionsSource = { minVersions },
        storeReleaseSource = { storeRelease },
        appVersion = { oldVersion }
    )

    // Offline: the next launch checks again instead of waiting a day
    @Test
    fun aFailedMinVersionFetchDoesNotStoreTheCheckTime() = runTest {
        val decision = checker(minVersions = offline).check(nowEpochMillis = NOW)

        assertEquals(UpdateDecision.None, decision)
        assertNull(store.longs[LAST_CHECK])
        assertNull(store.booleans[LAST_REQUIRED])
    }

    @Test
    fun aSecondOfflineLaunchStillChecks() = runTest {
        val offlineChecker = checker(minVersions = offline)
        offlineChecker.check(nowEpochMillis = NOW)

        assertEquals(UpdateDecision.None, offlineChecker.check(nowEpochMillis = NOW + ONE_HOUR))
    }

    // An empty MIN_VERSION_URL is not a failure: there is nothing to ask
    @Test
    fun anUnconfiguredMinVersionUrlStillStoresTheCheckTime() = runTest {
        checker(minVersions = Result.success(null)).check(nowEpochMillis = NOW)

        assertEquals(NOW, store.longs[LAST_CHECK])
        assertEquals(false, store.booleans[LAST_REQUIRED])
    }

    @Test
    fun aSuccessfulFetchStoresTheCheckTimeAndTheRequiredFlag() = runTest {
        val decision = checker(
            minVersions = Result.success(MinVersions(android = 2, ios = "2.0.0"))
        ).check(nowEpochMillis = NOW)

        assertIs<UpdateDecision.Required>(decision)
        assertEquals(NOW, store.longs[LAST_CHECK])
        assertEquals(true, store.booleans[LAST_REQUIRED])
    }

    @Test
    fun aCheckedDayDoesNotCheckAgain() = runTest {
        checker().check(nowEpochMillis = NOW)

        assertNull(checker(minVersions = offline).check(nowEpochMillis = NOW + ONE_HOUR))
    }

    // The store lookup only runs on iOS; its fetcher answers success(null) elsewhere
    @Test
    fun onIosAFailedStoreLookupDoesNotStoreTheCheckTime() = runTest {
        checker(storeRelease = offline).check(nowEpochMillis = NOW)

        assertNull(store.longs[LAST_CHECK])
        assertNull(store.booleans[LAST_REQUIRED])
    }

    // What answered still decides: the minimum version applies even when the store lookup failed
    @Test
    fun theDecisionIsStillReturnedWhenOneSourceFails() = runTest {
        val decision = checker(
            minVersions = Result.success(MinVersions(android = 2, ios = "2.0.0")),
            storeRelease = offline
        ).check(nowEpochMillis = NOW)

        assertIs<UpdateDecision.Required>(decision)
        assertNull(store.longs[LAST_CHECK])
    }

    // After a Required decision every launch checks, even within the day
    @Test
    fun aRequiredDecisionChecksAgainOnTheNextLaunch() = runTest {
        checker(minVersions = Result.success(MinVersions(android = 2, ios = "2.0.0"))).check(nowEpochMillis = NOW)

        assertIs<UpdateDecision.Required>(
            checker(minVersions = Result.success(MinVersions(android = 2, ios = "2.0.0")))
                .check(nowEpochMillis = NOW + ONE_HOUR)
        )
    }
}

private class FakeUpdateCheckStore : UpdateCheckStore {
    val longs = mutableMapOf<String, Long>()
    val booleans = mutableMapOf<String, Boolean>()

    override fun getLong(key: String): Long? = longs[key]

    override fun putLong(
        key: String,
        value: Long
    ) {
        longs[key] = value
    }

    override fun getBoolean(
        key: String,
        default: Boolean
    ): Boolean = booleans[key] ?: default

    override fun putBoolean(
        key: String,
        value: Boolean
    ) {
        booleans[key] = value
    }
}
