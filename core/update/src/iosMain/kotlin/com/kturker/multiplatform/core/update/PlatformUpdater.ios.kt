package com.kturker.multiplatform.core.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private val log = Logger.withTag("Update")

actual val currentUpdatePlatform: UpdatePlatform = UpdatePlatform.Ios

internal actual fun currentBundleId(): String? = NSBundle.mainBundle.bundleIdentifier

// iOS has no download to wait for: the App Store installs the update
@Composable
actual fun rememberPlatformUpdater(onDownloaded: () -> Unit): PlatformUpdater =
    remember { IosPlatformUpdater() }

internal class IosPlatformUpdater : PlatformUpdater {

    // iOS's recommended update is the "Update available" dialog (UpdateDecision.Available)
    override suspend fun startRecommendedUpdate(): Boolean = false

    override fun completeUpdate() = Unit

    override suspend fun startRequiredUpdate(url: String?) = openStore(url)

    // No URL: the App Store lookup found no page (the app is not on the App Store yet)
    override fun openStore(url: String?) {
        val storeUrl = url?.let { NSURL.URLWithString(it) }
        if (storeUrl == null) {
            log.w { "No App Store page for this app" }
            return
        }
        UIApplication.sharedApplication.openURL(
            storeUrl,
            options = emptyMap<Any?, Any?>(),
            completionHandler = { opened -> if (!opened) log.w { "Could not open the App Store page" } }
        )
    }
}
