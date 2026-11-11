package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.insights.consent.AnalyticsConsent
import com.kturker.multiplatform.core.insights.consent.InsightsConsentController
import org.koin.compose.koinInject

/**
 * The privacy switches for your settings screen: usage data (Analytics, off until the user allows it) and crash
 * reports (Crashlytics, on until the user turns it off). Each change is saved and reaches Firebase at once. Without
 * the Firebase config the answers are still kept, for when you add it. Keep it on your settings screen: the user must
 * be able to change both answers at any time.
 */
@Composable
fun InsightsConsentSettings(
    modifier: Modifier = Modifier,
    controller: InsightsConsentController = koinInject()
) {
    val texts = LocalInsightsConsentTexts.current
    val analytics by controller.analyticsConsent.collectAsState()
    val crashReports by controller.crashReportsEnabled.collectAsState()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(texts.settingsTitle, style = MaterialTheme.typography.titleSmall)
        ConsentSwitch(
            label = texts.analytics,
            description = texts.analyticsDescription,
            checked = analytics == AnalyticsConsent.Granted,
            onCheckedChange = { allowed -> if (allowed) controller.grantAnalytics() else controller.denyAnalytics() }
        )
        ConsentSwitch(
            label = texts.crashReports,
            description = texts.crashReportsDescription,
            checked = crashReports,
            onCheckedChange = controller::setCrashReports
        )
    }
}

@Composable
private fun ConsentSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // The whole row toggles, and reads as one switch to TalkBack / VoiceOver
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
