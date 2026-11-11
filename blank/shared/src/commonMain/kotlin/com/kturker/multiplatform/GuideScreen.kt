package com.kturker.multiplatform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.about.licenses.LicensesDestination
// cmpose:insights
import com.kturker.multiplatform.core.insights.consent.ui.InsightsConsentSettings
// /cmpose:insights
import com.kturker.multiplatform.core.navigation.FeatureEntry
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.rememberReviewRequester
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The guide, on the start destination (GuideDestination.kt) until your own home screen takes its place
 * (MainScreen.kt). Its own texts stay English: they are for you, the developer; the About dialog follows
 * [LocalAboutTexts] (AboutTexts.kt).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuideScreen(
    entries: List<FeatureEntry>,
    onEntryClick: (FeatureEntry) -> Unit,
    onDocumentationClick: () -> Unit
) {
    // A demo: in your app, call ReviewPrompter.recordPositiveEvent() after a moment the user wanted
    val reviewRequester = rememberReviewRequester()
    val scope = rememberCoroutineScope()
    // About: the app's version, your legal links once filled in (core/about) and the open source licences
    val appInfo = koinInject<AppInfo>()
    val navigationManager = koinInject<NavigationManager>()
    var showAbout by remember { mutableStateOf(false) }
    val aboutTexts = LocalAboutTexts.current

    Scaffold(topBar = { TopAppBar(title = { Text("New CMP Project") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🚀 Your Project is Ready!")
            Spacer(modifier = Modifier.height(16.dp))

            Text("Core modules have been included.")
            Text("To learn how to use them:")

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onDocumentationClick) {
                Text("📚 Read Documentation")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(onClick = { scope.launch { reviewRequester.requestReview() } }) {
                Text("Rate the app")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { showAbout = true }) {
                Text("About")
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Your feature modules:")
                entries.forEach { entry ->
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { onEntryClick(entry) }) {
                        Text("Open ${entry.title}")
                    }
                }
            }
        }
    }

    if (showAbout) {
        AboutDialog(
            appInfo = appInfo,
            onLicensesClick = {
                showAbout = false
                navigationManager.navigate(LicensesDestination(title = aboutTexts.licenses))
            },
            onDismiss = { showAbout = false }
        )
    }
}

@Composable
private fun AboutDialog(
    appInfo: AppInfo,
    onLicensesClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val texts = LocalAboutTexts.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(texts.title) },
        text = {
            // Scrolls: at a large font size on a small phone the links and switches would not fit
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(texts.version(appInfo.version.displayText))
                appInfo.legalLinks.forEach { link ->
                    TextButton(onClick = { uriHandler.openUri(link.url) }) {
                        Text(if (link.kind == LegalLinkKind.PrivacyPolicy) texts.privacyPolicy else texts.terms)
                    }
                }
                // cmpose:insights
                // The privacy switches: usage data (Analytics) and crash reports (core/insights)
                InsightsConsentSettings(modifier = Modifier.padding(top = 16.dp))
                // /cmpose:insights
            }
        },
        confirmButton = {
            TextButton(onClick = onLicensesClick) {
                Text(texts.licenses)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(texts.close)
            }
        }
    )
}
