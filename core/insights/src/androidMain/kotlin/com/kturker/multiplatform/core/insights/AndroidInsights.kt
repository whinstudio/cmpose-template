package com.kturker.multiplatform.core.insights

import android.content.Context
import android.os.Bundle
import co.touchlab.kermit.Logger
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.mp.KoinPlatform

/**
 * Firebase starts itself (FirebaseInitProvider) from google-services.json, before the Application. Without that
 * file there is no Firebase app, and Crashlytics and Analytics would throw. Logs nothing through Kermit: see
 * InsightsLogWriter.
 */
internal class AndroidInsights(private val crashlytics: FirebaseCrashlytics, private val analytics: FirebaseAnalytics) :
    Insights {

    override val isEnabled = true

    override fun logEvent(
        name: String,
        params: Map<String, Any>
    ) = analytics.logEvent(name, params.toBundle())

    override fun setUserId(id: String?) {
        // Crashlytics takes no null: an empty id clears it
        crashlytics.setUserId(id.orEmpty())
        analytics.setUserId(id)
    }

    override fun recordNonFatal(
        throwable: Throwable,
        message: String?
    ) {
        message?.let(crashlytics::log)
        crashlytics.recordException(throwable)
    }

    override fun log(message: String) = crashlytics.log(message)

    override fun setCollectionEnabled(enabled: Boolean) {
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
        analytics.setAnalyticsCollectionEnabled(enabled)
    }
}

// Analytics takes strings, longs and doubles
private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
    forEach { (key, value) ->
        when (value) {
            is String -> putString(key, value)
            is Int -> putLong(key, value.toLong())
            is Long -> putLong(key, value)
            is Float -> putDouble(key, value.toDouble())
            is Double -> putDouble(key, value)
            else -> putString(key, value.toString())
        }
    }
}

internal actual fun platformInsights(): Insights {
    val context = KoinPlatform.getKoinOrNull()?.getOrNull<Context>()
    if (context == null || FirebaseApp.getApps(context).isEmpty()) {
        // Info, not a warning: a warning would go to the insights writer
        Logger.withTag("Insights").i { "No Firebase config: Crashlytics and Analytics are off" }
        return NoOpInsights
    }
    return AndroidInsights(crashlytics = FirebaseCrashlytics.getInstance(), analytics = Firebase.analytics)
}
