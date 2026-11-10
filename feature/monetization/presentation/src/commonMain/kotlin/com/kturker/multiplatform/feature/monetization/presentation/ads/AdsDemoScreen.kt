package com.kturker.multiplatform.feature.monetization.presentation.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.ads.BannerAd
import com.kturker.multiplatform.core.ads.rememberAdsController
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun AdsDemoScreenRoute(viewModel: AdsDemoViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val controller = rememberAdsController()

    // Consent is state, not a one-shot command: a recreated screen (rotation, dark mode, language) runs the
    // flow again until a result has reached the ViewModel.
    LaunchedEffect(viewModel, controller) {
        if (!viewModel.uiState.value.consentResolved) {
            viewModel.onConsentResult(controller.gatherConsent())
        }
    }

    LaunchedEffect(viewModel, controller) {
        // The ViewModel runs each call in its own scope, so the result of an ad on screen survives a recreated
        // screen (rotation). The controller opens a loaded ad only while this screen is resumed: after Back, or
        // once a recreation has destroyed this screen, an ad that has not opened is dropped.
        viewModel.commands.collect { command ->
            when (command) {
                AdCommand.ShowInterstitial -> viewModel.runAd { controller.showInterstitial() }
                AdCommand.ShowRewarded -> viewModel.runAd { controller.showRewarded() }
                AdCommand.ShowPrivacyOptions -> viewModel.runPrivacyOptions { controller.showPrivacyOptions() }
            }
        }
    }

    AdsDemoScreen(uiState = uiState, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdsDemoScreen(
    uiState: AdsDemoUiState,
    actions: AdsDemoActions
) {
    val texts = LocalAdsDemoTexts.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(texts.title) },
                navigationIcon = {
                    TextButton(onClick = actions::onNavigateUp) {
                        Text(texts.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            // Scrolls so that landscape keeps every button and message reachable
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.adFree == true) {
                Text(texts.premiumAdsOff)
            } else if (uiState.showBanner) {
                BannerAd()
            }
            Button(onClick = actions::onShowInterstitialClick, enabled = uiState.canShowInterstitial) {
                Text(texts.showInterstitial)
            }
            Button(onClick = actions::onWatchRewardedClick, enabled = uiState.canShowAds) {
                Text(texts.watchRewarded)
            }
            if (uiState.showPrivacyOptionsButton) {
                TextButton(onClick = actions::onPrivacyOptionsClick, enabled = !uiState.adBusy) {
                    Text(texts.privacyOptions)
                }
            }
            Text(texts.coinsLabel(uiState.coins))
            uiState.message?.let { message -> Text(texts.message(message)) }
            Text(texts.testAdsNote)
        }
    }
}
