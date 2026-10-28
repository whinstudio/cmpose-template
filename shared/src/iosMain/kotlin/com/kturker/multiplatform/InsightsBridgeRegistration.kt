@file:Suppress("Filename")

package com.kturker.multiplatform

import com.kturker.multiplatform.core.insights.IosInsights
import com.kturker.multiplatform.core.insights.IosInsightsBridge

// Called from iOSApp.init(): the Swift InsightsBridge wraps Firebase Crashlytics and Analytics.
fun registerInsightsBridge(bridge: IosInsightsBridge) = IosInsights.register(bridge)
