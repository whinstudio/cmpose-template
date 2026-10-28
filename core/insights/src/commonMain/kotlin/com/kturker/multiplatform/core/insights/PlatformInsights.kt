@file:Suppress("Filename")

package com.kturker.multiplatform.core.insights

/** Android: Firebase's own SDKs, when Firebase has an app; iOS: the Swift bridge (IosInsightsBridge). */
internal expect fun platformInsights(): Insights
