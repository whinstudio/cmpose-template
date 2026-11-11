@file:Suppress("Filename")

package com.kturker.multiplatform.core.insights.consent

/** Android: the app's SharedPreferences; iOS: NSUserDefaults. */
internal expect fun platformConsentStore(): InsightsConsentStore
