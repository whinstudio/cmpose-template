package com.kturker.multiplatform.core.insights.consent.ui

import kotlin.test.Test
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
}
