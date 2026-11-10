package com.kturker.multiplatform.core.ads

import android.content.pm.ApplicationInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberAdsOffForAutomation(): Boolean {
    val activity = requireNotNull(LocalActivity.current) { "Ads need the hosting Activity" }
    return remember(activity) {
        val intent = activity.intent
        automationGate.adsOff(
            flagPassed = intent.getBooleanExtra(AUTOMATION_FLAG, false) ||
                intent.getStringExtra(AUTOMATION_FLAG).toBoolean(),
            automationBuild = (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        )
    }
}
