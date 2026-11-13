package com.kturker.multiplatform.core.insights.consent

import com.kturker.multiplatform.core.insights.di.InsightsModule
import com.kturker.multiplatform.core.navigation.LaunchPrompt
import org.koin.dsl.koinApplication
import org.koin.ksp.generated.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

// With insights in the app, Koin hands the analytics prompt to whoever waits for launch prompts (the first-launch
// paywall collects them with getAll); without the module there is none, and nothing waits
class AnalyticsLaunchPromptBindingTest {

    private val app = koinApplication { modules(InsightsModule().module) }

    @AfterTest
    fun tearDown() {
        app.close()
    }

    @Test
    fun insightsAddsTheAnalyticsPromptAsALaunchPrompt() {
        val prompts = app.koin.getAll<LaunchPrompt>()

        assertEquals(1, prompts.size)
        val prompt = assertIs<AnalyticsLaunchPrompt>(prompts.single())
        assertSame(app.koin.get<InsightsConsentController>().analyticsPromptPending, prompt.pending)
    }
}
