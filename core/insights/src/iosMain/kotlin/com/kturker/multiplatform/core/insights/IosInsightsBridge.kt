package com.kturker.multiplatform.core.insights

import co.touchlab.crashkios.crashlytics.enableCrashlytics
import co.touchlab.crashkios.crashlytics.setCrashlyticsUnhandledExceptionHook

// Implemented in Swift on top of FirebaseCrashlytics and FirebaseAnalytics (Swift Package Manager) and registered
// at launch: Kotlin/Native cannot link the Firebase SDK directly.
interface IosInsightsBridge {
    // True when the app found GoogleService-Info.plist and called FirebaseApp.configure()
    val isConfigured: Boolean

    fun logEvent(
        name: String,
        params: Map<String, Any>
    )

    fun setUserId(id: String?)

    // A Kotlin throwable as Crashlytics' ExceptionModel: its type, its message and its stack frames
    fun recordNonFatal(
        name: String,
        message: String?,
        stack: List<String>
    )

    fun log(message: String)

    fun setCollectionEnabled(enabled: Boolean)
}

object IosInsights {
    private var bridge: IosInsightsBridge? = null

    // CrashKiOS: an unhandled Kotlin exception reaches Crashlytics with its Kotlin type, message and stack. Replaced
    // in tests, whose binaries do not link Crashlytics.
    internal var installKotlinCrashReporting: () -> Unit = {
        enableCrashlytics()
        setCrashlyticsUnhandledExceptionHook()
    }

    fun register(bridge: IosInsightsBridge) {
        this.bridge = bridge
        if (bridge.isConfigured) installKotlinCrashReporting()
    }

    internal fun bridgeOrNull(): IosInsightsBridge? = bridge
}
