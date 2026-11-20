package com.kturker.multiplatform.core.insights.consent.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsConsentTextsTest {

    // The English defaults are what a project without multilang shows: none of them may be empty
    @Test
    fun theEnglishDefaultsAreFilledIn() {
        val texts = InsightsConsentTexts()

        listOf(
            texts.promptTitle,
            texts.promptMessage,
            texts.allow,
            texts.deny,
            texts.settingsTitle,
            texts.analytics,
            texts.analyticsDescription,
            texts.crashReports,
            texts.crashReportsDescription
        ).forEach { assertTrue(it.isNotBlank(), "$texts") }
    }

    // No place in the app: the prompt is true in the showcase (settings screen) and a blank project (About dialog)
    @Test
    fun thePromptDoesNotNameWhereToChangeTheAnswer() {
        assertEquals(
            "Can we collect usage data, such as which screens you open, to improve the app? " +
                "You can change this at any time.",
            InsightsConsentTexts().promptMessage
        )
    }
}
