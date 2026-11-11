package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kturker.multiplatform.core.insights.consent.AnalyticsConsent
import com.kturker.multiplatform.core.insights.consent.InsightsConsentController
import org.koin.compose.koinInject

/**
 * Asks once whether Analytics may collect usage data; Analytics stays off until the user allows it. Shown while
 * there is no answer and the app has its Firebase config ([InsightsConsentController.needsAnalyticsPrompt]); gone as
 * soon as the user answers. Tapping outside or back is no answer: the prompt goes for this run and comes back at the
 * next launch. Put it inside the app theme, once (MainScreen.kt).
 */
@Composable
fun InsightsConsentDialog(controller: InsightsConsentController = koinInject()) {
    // Collected so the prompt recomposes, and goes, once an answer is saved (needsAnalyticsPrompt is no flow)
    val consent by controller.analyticsConsent.collectAsState()
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (consent != AnalyticsConsent.Unknown || dismissed || !controller.needsAnalyticsPrompt) return

    val texts = LocalInsightsConsentTexts.current
    AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text(texts.promptTitle) },
        text = { Text(texts.promptMessage) },
        confirmButton = {
            TextButton(onClick = controller::grantAnalytics) {
                Text(texts.allow)
            }
        },
        dismissButton = {
            TextButton(onClick = controller::denyAnalytics) {
                Text(texts.deny)
            }
        }
    )
}
