package com.kturker.multiplatform.core.insights.consent

import com.kturker.multiplatform.core.navigation.LaunchPrompt
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Single

/**
 * The analytics prompt as a launch prompt: the first-launch paywall (feature/monetization) waits while it is due and
 * neither answered nor dismissed, so the two dialogs never open on top of each other.
 */
@Single(binds = [LaunchPrompt::class])
internal class AnalyticsLaunchPrompt(controller: InsightsConsentController) : LaunchPrompt {
    override val pending: StateFlow<Boolean> = controller.analyticsPromptPending
}
