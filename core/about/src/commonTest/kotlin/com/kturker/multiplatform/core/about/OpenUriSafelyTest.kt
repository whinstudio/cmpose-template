package com.kturker.multiplatform.core.about

import androidx.compose.ui.platform.UriHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenUriSafelyTest {

    // Android throws ActivityNotFoundException (a RuntimeException) when no browser is installed or allowed
    @Test
    fun openUriSafelyReturnsFalseWhenNoActivityHandlesIt() {
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                throw RuntimeException("No Activity found to handle Intent")
            }
        }

        assertFalse(handler.openUriSafely("https://example.com/privacy"))
    }

    @Test
    fun openUriSafelyReturnsFalseOnIllegalState() {
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                throw IllegalStateException("No browser")
            }
        }

        assertFalse(handler.openUriSafely("https://example.com/terms"))
    }

    @Test
    fun openUriSafelyOpensTheUri() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }

        assertTrue(handler.openUriSafely("https://example.com/privacy"))
        assertEquals(listOf("https://example.com/privacy"), opened)
    }
}
