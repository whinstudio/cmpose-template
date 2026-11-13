package com.kturker.multiplatform.core.insights.consent.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class InsightsConsentTagsTest {

    // Walkthrough flows (Maestro) outside this repo tap the buttons by these exact strings: they must not change
    @Test
    fun tagsAreStable() {
        assertEquals("insights_consent_allow", InsightsConsentTags.ALLOW)
        assertEquals("insights_consent_deny", InsightsConsentTags.DENY)
    }
}
