package com.kturker.multiplatform.core.update

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.about.AppVersion
import com.kturker.multiplatform.core.about.currentAppVersion
import org.koin.core.annotation.Single
import kotlin.time.Clock

private const val KEY_LAST_CHECK = "update.lastCheck"
private const val KEY_LAST_REQUIRED = "update.lastRequired"
private const val STORE_LOOKUP_ATTEMPTS = 3
private const val STORE_LOOKUP_DELAY_MILLIS = 2_000L

private val log = Logger.withTag("Update")

// Reads MIN_VERSION_URL (when set) and, on iOS, the App Store lookup, then decides whether the user has to
// update, may update or neither. The sources answer success(null) when they are not set up (empty
// MIN_VERSION_URL, the store lookup off iOS) and failure when they got no usable answer; tests pass fakes.
@Single
class UpdateChecker(
    private val store: UpdateCheckStore,
    private val minVersionsSource: suspend () -> Result<MinVersions?> = ::fetchMinVersions,
    private val storeReleaseSource: suspend () -> Result<StoreRelease?> = ::fetchStoreRelease,
    private val appVersion: () -> AppVersion = ::currentAppVersion
) {

    // Null when no check is due today (see isCheckDue)
    suspend fun check(
        nowEpochMillis: Long = Clock.System.now().toEpochMilliseconds()
    ): UpdateDecision? {
        val due = isCheckDue(
            lastCheckEpochMillis = store.getLong(key = KEY_LAST_CHECK),
            nowEpochMillis = nowEpochMillis,
            lastWasRequired = store.getBoolean(key = KEY_LAST_REQUIRED, default = false)
        )
        if (!due) return null
        val storeRelease = storeReleaseSource()
        val minVersions = minVersionsSource()
        val decision = decideUpdate(
            current = appVersion(),
            minVersions = minVersions.getOrNull(),
            storeRelease = storeRelease.getOrNull(),
            platform = currentUpdatePlatform
        )
        // An offline launch is not a check: the next launch asks again instead of waiting a day
        if (minVersions.isSuccess && storeRelease.isSuccess) {
            store.putLong(key = KEY_LAST_CHECK, value = nowEpochMillis)
            store.putBoolean(key = KEY_LAST_REQUIRED, value = decision is UpdateDecision.Required)
        } else {
            log.w { "Update check got no answer, checks again next launch" }
        }
        log.i { "Update decision: ${decision.logName()}" }
        return decision
    }

    // Where "Update" leads when the server asks for a newer version (ForcedUpdateSignal): the App Store page on
    // iOS, null when the lookup finds none; null on Android, where Play's page is opened without a URL. The
    // screen's button does nothing without it, so a failed lookup is tried again a few times.
    suspend fun storeUrl(): String? {
        if (currentUpdatePlatform != UpdatePlatform.Ios) return null
        val url = retryOnNull(attempts = STORE_LOOKUP_ATTEMPTS, delayMs = STORE_LOOKUP_DELAY_MILLIS) {
            storeReleaseSource().getOrNull()?.storeUrl
        }
        if (url == null) log.w { "App Store lookup found no page after $STORE_LOOKUP_ATTEMPTS attempts" }
        return url
    }
}

private suspend fun fetchMinVersions(): Result<MinVersions?> {
    if (MIN_VERSION_URL.isBlank()) return Result.success(null)
    return fetchText(MIN_VERSION_URL)?.let(::parseMinVersions).toResult(source = "min-version file")
}

// Any answer counts: an app not on the App Store (yet, or not in the lookup's country) gets no result, and
// failing then would check on every launch for good
private suspend fun fetchStoreRelease(): Result<StoreRelease?> {
    if (currentUpdatePlatform != UpdatePlatform.Ios) return Result.success(null)
    val bundleId = currentBundleId() ?: return Result.success(null)
    val answer = fetchText(appStoreLookupUrl(bundleId)) ?: return noAnswer(source = "App Store lookup")
    return Result.success(parseStoreRelease(answer))
}

// Null: no connection or an answer that could not be read
private fun <T : Any> T?.toResult(source: String): Result<T?> =
    if (this != null) Result.success(this) else noAnswer(source)

private fun noAnswer(source: String): Result<Nothing> =
    Result.failure(IllegalStateException("No answer from $source"))

// Written out: R8 renames the classes in a release build
private fun UpdateDecision.logName(): String = when (this) {
    UpdateDecision.None -> "None"
    is UpdateDecision.Available -> "Available"
    is UpdateDecision.Required -> "Required"
}
