package com.kturker.multiplatform.core.multilang.resource

import com.kturker.multiplatform.core.multilang.resource.res.languageResources
import kotlin.test.Test
import kotlin.test.assertTrue

class InsightsConsentStringsTest {

    // The analytics prompt and the two privacy switches (core/insights' InsightsConsentTexts), in every language
    @Test
    fun everyLanguageFillsTheConsentStrings() {
        languageResources.forEach { (language, strings) ->
            listOf(
                strings.insightsPromptTitle,
                strings.insightsPromptMessage,
                strings.insightsAllow,
                strings.insightsDeny,
                strings.insightsSettingsTitle,
                strings.insightsAnalytics,
                strings.insightsAnalyticsDescription,
                strings.insightsCrashReports,
                strings.insightsCrashReportsDescription
            ).forEach { assertTrue(it.isNotBlank(), "$language") }
        }
    }
}
