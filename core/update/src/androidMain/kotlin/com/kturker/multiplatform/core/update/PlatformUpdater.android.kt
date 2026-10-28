package com.kturker.multiplatform.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import co.touchlab.kermit.Logger
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo

private val log = Logger.withTag("Update")

actual val currentUpdatePlatform: UpdatePlatform = UpdatePlatform.Android

internal actual fun currentBundleId(): String? = null

@Composable
actual fun rememberPlatformUpdater(onDownloaded: () -> Unit): PlatformUpdater {
    val activity = requireNotNull(LocalActivity.current) { "In-app updates need the hosting Activity" }
    val currentOnDownloaded by rememberUpdatedState(onDownloaded)
    // Play shows its own screens; the result only says whether the user accepted
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) log.i { "Update flow closed: result ${result.resultCode}" }
    }
    val updater = remember(activity, launcher) {
        AndroidPlatformUpdater(activity = activity, launcher = launcher, onDownloaded = { currentOnDownloaded() })
    }
    DisposableEffect(updater) {
        updater.listen()
        onDispose { updater.stopListening() }
    }
    return updater
}

// Play offers an update only to an app installed from Google Play (an internal test track works); on an
// emulator or a sideloaded build the check fails and is skipped.
internal class AndroidPlatformUpdater(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<IntentSenderRequest>,
    private val onDownloaded: () -> Unit
) : PlatformUpdater {

    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(activity)

    // A second "Update" tap before Play's screen covers the app
    private val requiredUpdate = OneAtATime()

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) onDownloaded()
    }

    fun listen() {
        manager.registerListener(listener)
    }

    fun stopListening() {
        manager.unregisterListener(listener)
    }

    override suspend fun startRecommendedUpdate(): Boolean {
        val info = try {
            manager.requestAppUpdateInfo()
        } catch (e: InstallException) {
            log.w { "Play update check unavailable: error ${e.errorCode}" }
            return false
        }
        // Downloaded in an earlier session but not installed yet: offer the restart again
        if (info.installStatus() == InstallStatus.DOWNLOADED) {
            onDownloaded()
            return true
        }
        val offered = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
            info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        if (!offered) return false
        return manager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    override fun completeUpdate() {
        manager.completeUpdate()
    }

    // Play installs and restarts the app itself; a cancelled flow returns here with the required screen still up
    override suspend fun startRequiredUpdate(url: String?) =
        requiredUpdate.run { startImmediateOrOpenStore(url) }

    private suspend fun startImmediateOrOpenStore(url: String?) {
        val info = try {
            manager.requestAppUpdateInfo()
        } catch (e: InstallException) {
            log.w { "Play update check unavailable: error ${e.errorCode}" }
            openStore(url)
            return
        }
        val availability = info.updateAvailability()
        val path = requiredUpdatePath(
            updateAvailable = availability == UpdateAvailability.UPDATE_AVAILABLE,
            updateInProgress = availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
            immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        )
        val started = path == RequiredUpdatePath.Immediate && manager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
        if (!started) openStore(url)
    }

    // The application ID is the package name: the Play Store app first, then the web page
    override fun openStore(url: String?) {
        val packageName = activity.packageName
        val opened = activity.open("market://details?id=$packageName") ||
            activity.open("https://play.google.com/store/apps/details?id=$packageName")
        if (!opened) log.w { "No app can open the Play Store page" }
    }
}

private fun Activity.open(uri: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
    true
} catch (_: ActivityNotFoundException) {
    false
}
