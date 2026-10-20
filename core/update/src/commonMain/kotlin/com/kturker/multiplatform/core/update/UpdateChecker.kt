package com.kturker.multiplatform.core.update

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.about.currentAppVersion
import org.koin.core.annotation.Single
import kotlin.time.Clock

private const val KEY_LAST_CHECK = "update.lastCheck"
private const val KEY_LAST_REQUIRED = "update.lastRequired"

private val log = Logger.withTag("Update")

// Reads MIN_VERSION_URL (when set) and, on iOS, the App Store lookup, then decides whether the user has to
// update, may update or neither
@Single
class UpdateChecker(private val store: UpdateCheckStore) {

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
        val storeRelease = if (currentUpdatePlatform == UpdatePlatform.Ios) fetchStoreRelease() else null
        val decision = decideUpdate(
            current = currentAppVersion(),
            minVersions = fetchMinVersions(),
            storeRelease = storeRelease,
            platform = currentUpdatePlatform
        )
        store.putLong(key = KEY_LAST_CHECK, value = nowEpochMillis)
        store.putBoolean(key = KEY_LAST_REQUIRED, value = decision is UpdateDecision.Required)
        log.i { "Update decision: ${decision.logName()}" }
        return decision
    }

    // Where "Update" leads when the server asks for a newer version (ForcedUpdateSignal): the App Store page on
    // iOS, null when the lookup finds none; null on Android, where Play's page is opened without a URL
    suspend fun storeUrl(): String? =
        if (currentUpdatePlatform == UpdatePlatform.Ios) fetchStoreRelease()?.storeUrl else null
}

private suspend fun fetchMinVersions(): MinVersions? {
    if (MIN_VERSION_URL.isBlank()) return null
    return fetchText(MIN_VERSION_URL)?.let(::parseMinVersions)
}

private suspend fun fetchStoreRelease(): StoreRelease? {
    val bundleId = currentBundleId() ?: return null
    return fetchText(appStoreLookupUrl(bundleId))?.let(::parseStoreRelease)
}

// Written out: R8 renames the classes in a release build
private fun UpdateDecision.logName(): String = when (this) {
    UpdateDecision.None -> "None"
    is UpdateDecision.Available -> "Available"
    is UpdateDecision.Required -> "Required"
}
