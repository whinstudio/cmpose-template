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
import com.kturker.multiplatform.core.purchases.PaywallPackage
import org.koin.compose.viewmodel.koinViewModel

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
    val texts = LocalPaywallTexts.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(texts.title) },
                navigationIcon = { TextButton(onClick = actions::onDismiss) { Text(texts.notNow) } }
            )
        }
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
        when {
            !uiState.isAvailable -> CenteredMessage(text = texts.purchasesDisabled, modifier = modifier)

            uiState.loading -> Loading(modifier = modifier)

            uiState.offer == null -> PlansError(
                message = texts.message(uiState.error ?: PaywallError.PlansUnavailable),
                retryLabel = texts.retry,
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
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text(retryLabel) }
    }
}

@Composable
private fun Plans(
    uiState: PaywallUiState,
    actions: PaywallActions,
    modifier: Modifier
) {
    val texts = LocalPaywallTexts.current
    val benefits = listOf(texts.benefitRemoveAds, texts.benefitOne, texts.benefitTwo)
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = texts.headline, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        benefits.forEach { benefit -> Text(text = "✓ $benefit") }
        uiState.packages.forEach { pkg ->
            PlanCard(
                pkg = pkg,
                selected = pkg.id == uiState.selectedPackageId,
                onClick = { actions.onPackageSelected(pkg.id) }
            )
        }
        uiState.error?.let { error -> Text(text = texts.message(error), color = MaterialTheme.colorScheme.error) }
        Button(onClick = actions::onPurchaseClick, enabled = uiState.canPurchase, modifier = Modifier.fillMaxWidth()) {
            if (uiState.inProgress) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text(texts.continueLabel)
            }
        }
        TextButton(
            onClick = actions::onRestoreClick,
            enabled = !uiState.inProgress,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(texts.restore)
        }
        if (uiState.showsRenewalTerms) {
            Text(text = texts.renewalTerms, style = MaterialTheme.typography.bodySmall)
        }
        LegalLinks(links = uiState.legalLinks)
        if (uiState.missingLegalLinks) {
            Text(
                text = texts.missingLegalLinks,
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
    val texts = LocalPaywallTexts.current
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    }
    OutlinedCard(onClick = onClick, border = border, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = texts.periodLabel(pkg.period), fontWeight = FontWeight.Bold)
                pkg.savingsPercent?.let { percent ->
                    Text(text = texts.saveLabel(percent), color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(text = texts.priceLabel(pkg.price, pkg.period))
            pkg.pricePerMonth?.let { perMonth ->
                Text(text = texts.perMonthLabel(perMonth), style = MaterialTheme.typography.bodySmall)
            }
            pkg.freeTrial?.let { trial ->
                Text(text = texts.freeTrialLabel(trial), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LegalLinks(links: List<LegalLink>) {
    val texts = LocalPaywallTexts.current
    val uriHandler = LocalUriHandler.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        links.forEach { link ->
            TextButton(onClick = { uriHandler.openUri(link.url) }) {
                Text(
                    when (link.kind) {
                        LegalLinkKind.PrivacyPolicy -> texts.privacyPolicy
                        LegalLinkKind.Terms -> texts.terms
                    }
                )
            }
        }
    }
}
