package com.kturker.multiplatform.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

// A launch argument "-compassCapture true" reaches NSUserDefaults; a bare "compassCapture" only the argument list.
@Composable
internal actual fun rememberAdsOffForAutomation(): Boolean = remember {
    val process = NSProcessInfo.processInfo
    automationGate.adsOff(
        flagPassed = AUTOMATION_FLAG in process.arguments ||
            NSUserDefaults.standardUserDefaults.boolForKey(AUTOMATION_FLAG),
        automationBuild = process.environment["SIMULATOR_UDID"] != null
    )
}
