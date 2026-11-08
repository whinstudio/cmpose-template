package com.kturker.multiplatform.feature.monetization.presentation.paywall

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.purchases.PackagePeriod
import com.kturker.multiplatform.core.purchases.PaywallPackage
import org.koin.compose.viewmodel.koinViewModel

private const val PURCHASES_DISABLED_MESSAGE =
    "Purchases are disabled in release builds that use a RevenueCat Test Store key. " +
        "Add your App Store / Play Store keys in PurchasesKeys.android.kt and PurchasesKeys.ios.kt."

// Shown under the plans whenever one of them renews: the App Store and Google Play want these terms on the paywall
private const val RENEWAL_TERMS =
    "Subscriptions renew automatically unless cancelled at least 24 hours before the end of the current period. " +
        "Manage or cancel them in your App Store or Google Play account settings."

// Debug builds only, until LegalUrls.kt has both links
private const val MISSING_LEGAL_LINKS =
    "Set PRIVACY_POLICY_URL and TERMS_URL in LegalUrls.kt before you ship: the stores want both links on this screen."

// Placeholder benefits: list what premium unlocks in your app
private val PREMIUM_BENEFITS = listOf("Remove ads", "Premium feature one", "Premium feature two")

/**
 * A plain paywall on PurchasesManager: the current offering's plans, Continue, Restore purchases, Not now and the
 * legal links. Deliberately simple: redraw it in your app's design and keep PaywallViewModel's behaviour.
 */
@Composable
internal fun PaywallScreenRoute(viewModel: PaywallViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PaywallScreen(uiState = uiState, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallScreen(
    uiState: PaywallUiState,
    actions: PaywallActions
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium") },
                navigationIcon = { TextButton(onClick = actions::onDismiss) { Text("Not now") } }
            )
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        when {
            !uiState.isAvailable -> CenteredMessage(text = PURCHASES_DISABLED_MESSAGE, modifier = modifier)

            uiState.loading -> Loading(modifier = modifier)

            uiState.offer == null -> PlansError(
                message = uiState.error.orEmpty(),
                onRetry = actions::onRetry,
                modifier = modifier
            )

            else -> Plans(uiState = uiState, actions = actions, modifier = modifier)
        }
    }
}

@Composable
private fun Loading(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlansError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Try again") }
    }
}

@Composable
private fun Plans(
    uiState: PaywallUiState,
    actions: PaywallActions,
    modifier: Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Unlock Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        PREMIUM_BENEFITS.forEach { benefit -> Text(text = "✓ $benefit") }
        uiState.packages.forEach { pkg ->
            PlanCard(
                pkg = pkg,
                selected = pkg.id == uiState.selectedPackageId,
                onClick = { actions.onPackageSelected(pkg.id) }
            )
        }
        uiState.error?.let { error -> Text(text = error, color = MaterialTheme.colorScheme.error) }
        Button(onClick = actions::onPurchaseClick, enabled = uiState.canPurchase, modifier = Modifier.fillMaxWidth()) {
            if (uiState.inProgress) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Continue")
        }
        TextButton(
            onClick = actions::onRestoreClick,
            enabled = !uiState.inProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore purchases")
        }
        if (uiState.showsRenewalTerms) {
            Text(text = RENEWAL_TERMS, style = MaterialTheme.typography.bodySmall)
        }
        LegalLinks(links = uiState.legalLinks)
        if (uiState.missingLegalLinks) {
            Text(
                text = MISSING_LEGAL_LINKS,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun PlanCard(
    pkg: PaywallPackage,
    selected: Boolean,
    onClick: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    OutlinedCard(onClick = onClick, border = border, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = periodLabel(pkg.period), fontWeight = FontWeight.Bold)
                pkg.savingsPercent?.let { percent ->
                    Text(text = "Save $percent%", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(text = pkg.price + priceSuffix(pkg.period))
            pkg.pricePerMonth?.let { perMonth ->
                Text(text = "$perMonth/month", style = MaterialTheme.typography.bodySmall)
            }
            pkg.freeTrial?.let { trial -> Text(text = "$trial free", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun LegalLinks(links: List<LegalLink>) {
    val uriHandler = LocalUriHandler.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        links.forEach { link ->
            TextButton(onClick = { uriHandler.openUri(link.url) }) {
                Text(
                    when (link.kind) {
                        LegalLinkKind.PrivacyPolicy -> "Privacy Policy"
                        LegalLinkKind.Terms -> "Terms of Use"
                    }
                )
            }
        }
    }
}

private fun periodLabel(period: PackagePeriod): String = when (period) {
    PackagePeriod.WEEKLY -> "Weekly"
    PackagePeriod.MONTHLY -> "Monthly"
    PackagePeriod.ANNUAL -> "Annual"
    PackagePeriod.LIFETIME -> "Lifetime"
    PackagePeriod.OTHER -> "Plan"
}

private fun priceSuffix(period: PackagePeriod): String = when (period) {
    PackagePeriod.WEEKLY -> " / week"
    PackagePeriod.MONTHLY -> " / month"
    PackagePeriod.ANNUAL -> " / year"
    PackagePeriod.LIFETIME -> " once"
    PackagePeriod.OTHER -> ""
}
