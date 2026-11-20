package com.kturker.multiplatform.core.about

import androidx.compose.ui.platform.UriHandler
import co.touchlab.kermit.Logger

private val log = Logger.withTag("About")

/**
 * Opens [uri] like [UriHandler.openUri], without crashing when nothing can open it: on Android a device without a
 * browser, or a work profile that blocks one, throws ActivityNotFoundException. Returns false then, and the tap does
 * nothing visible.
 */
fun UriHandler.openUriSafely(uri: String): Boolean = try {
    openUri(uri)
    true
} catch (e: RuntimeException) {
    // The type only: the URI is the app's own link, but the message can carry the full intent
    log.w { "No app could open a link: ${e::class.simpleName}" }
    false
}
