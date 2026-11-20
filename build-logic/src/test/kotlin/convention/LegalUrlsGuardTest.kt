package convention

import kotlin.test.Test
import kotlin.test.assertEquals

class LegalUrlsGuardTest {
    @Test
    fun aBlankValueIsEmpty() {
        val source = legalUrls(privacy = "\"\"", terms = "\"https://example.com/terms\"")

        assertEquals(listOf("PRIVACY_POLICY_URL"), emptyLegalUrls(source))
    }

    @Test
    fun aWhitespaceValueIsEmpty() {
        val source = legalUrls(privacy = "\"https://example.com/privacy\"", terms = "\"   \"")

        assertEquals(listOf("TERMS_URL"), emptyLegalUrls(source))
    }

    @Test
    fun aFilledValueIsNotEmpty() {
        val source = legalUrls(privacy = "\"https://example.com/privacy\"", terms = "\"https://example.com/terms\"")

        assertEquals(emptyList(), emptyLegalUrls(source))
    }

    @Test
    fun aConstantTheRegexDoesNotFindCountsAsFilled() {
        // Built another way (and a TODO mentioning the name), so the pattern finds no literal and nothing is reported
        val source = """
            // TODO: set PRIVACY_POLICY_URL = ""
            val PRIVACY_POLICY_URL: String = buildUrl("privacy")
            const val TERMS_URL = BASE + "/terms"
        """.trimIndent()

        assertEquals(emptyList(), emptyLegalUrls(source))
    }

    @Test
    fun bothNamesAreReported() {
        val source = legalUrls(privacy = "\"\"", terms = "\"\"")

        assertEquals(listOf("PRIVACY_POLICY_URL", "TERMS_URL"), emptyLegalUrls(source))
    }

    private fun legalUrls(privacy: String, terms: String): String = """
        package com.example.core.about

        object LegalUrls {
            const val PRIVACY_POLICY_URL: String = $privacy
            const val TERMS_URL = $terms
        }
    """.trimIndent()
}
