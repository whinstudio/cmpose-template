package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.kturker.multiplatform.core.insights.consent.InsightsConsentController
import org.koin.compose.koinInject

/**
 * Asks once whether Analytics may collect usage data; Analytics stays off until the user allows it. Shown while
 * there is no answer and the app has its Firebase config ([InsightsConsentController.analyticsPromptPending]); gone
 * as soon as the user answers. Tapping outside or back is no answer: the prompt goes for this run and comes back at
 * the next launch. The first-launch paywall waits until it is gone. Put it inside the app theme, once (MainScreen.kt).
 */
@Composable
fun InsightsConsentDialog(controller: InsightsConsentController = koinInject()) {
    val pending by controller.analyticsPromptPending.collectAsState()
    if (!pending) return

    val texts = LocalInsightsConsentTexts.current
    AlertDialog(
        // Kept in the controller, not here: the first-launch paywall waits for it too
        onDismissRequest = controller::dismissAnalyticsPrompt,
        // The dialog has its own window, so it does not inherit a resource-id setting from the app's root
        modifier = Modifier.testTagsAsResourceId(),
        title = { Text(texts.promptTitle) },
        text = { Text(texts.promptMessage) },
        confirmButton = {
            TextButton(
                onClick = controller::grantAnalytics,
                modifier = Modifier.testTag(InsightsConsentTags.ALLOW)
            ) {
                Text(texts.allow)
            }
        },
        dismissButton = {
            TextButton(
                onClick = controller::denyAnalytics,
                modifier = Modifier.testTag(InsightsConsentTags.DENY)
            ) {
                Text(texts.deny)
            }
        }
    )
}
