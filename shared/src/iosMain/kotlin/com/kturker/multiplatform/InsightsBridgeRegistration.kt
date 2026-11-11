@file:Suppress("Filename")

package com.kturker.multiplatform

import com.kturker.multiplatform.core.insights.IosInsights
import com.kturker.multiplatform.core.insights.IosInsightsBridge
import com.kturker.multiplatform.core.insights.consent.InsightsConsentController
import org.koin.mp.KoinPlatform

// Called from iOSApp.init(), after Koin starts: the Swift InsightsBridge wraps Firebase Crashlytics and Analytics.
// The saved consent is applied once the bridge is there, since before that no call reaches Firebase.
fun registerInsightsBridge(bridge: IosInsightsBridge) {
    IosInsights.register(bridge)
    KoinPlatform.getKoin().get<InsightsConsentController>().apply()
}
