package com.kturker.multiplatform.core.update

import androidx.compose.runtime.Composable

// What the update UI asks of the platform: Play In-App Updates on Android, the App Store on iOS
interface PlatformUpdater {
    // Android: starts Play's flexible update when Play offers one (true). iOS: false, its recommended update
    // is the "Update available" dialog (UpdateDecision.Available).
    suspend fun startRecommendedUpdate(): Boolean

    // Android: restarts the app into the downloaded update. iOS: nothing to do.
    fun completeUpdate()

    // The app's store page: Google Play on Android (url is null), url on iOS
    fun openStore(url: String?)

    // "Update" on the required screen. Android: Play's immediate update when Play offers one, else the store
    // page; a cancelled flow leaves the screen up. iOS: openStore(url).
    suspend fun startRequiredUpdate(url: String?)
}

// onDownloaded: Play has downloaded a flexible update and the app can restart into it (Android only)
@Composable
expect fun rememberPlatformUpdater(onDownloaded: () -> Unit): PlatformUpdater

expect val currentUpdatePlatform: UpdatePlatform

// The App Store lookup's key; null on Android
internal expect fun currentBundleId(): String?
