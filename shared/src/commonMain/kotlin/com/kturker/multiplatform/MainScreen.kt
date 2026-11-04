package com.kturker.multiplatform

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.image.ImageLoaderSetup
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel
import com.kturker.multiplatform.core.navigation.NavGraphProvider
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.ReviewRequestEffect
import com.kturker.multiplatform.core.update.AppUpdateEffect
import com.kturker.multiplatform.core.update.UpdateTexts
import com.kturker.multiplatform.feature.home.contract.HomeScreenDestination
import com.kturker.multiplatform.feature.monetization.presentation.paywall.FirstLaunchPaywall
import com.kturker.multiplatform.feature.onboarding.contract.OnboardingDestination
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@Composable
fun MainScreen(
    languageManager: LanguageManager = koinInject(),
    navigationManager: NavigationManager = koinInject()
) {
    // Coil's one image loader (core/image): caches, and Kermit logs in debug builds
    ImageLoaderSetup()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // The showcase's onboarding is done once a user's Home opens: the first-launch paywall waits for it
    val onboardingDone = backStackEntry?.destination?.hasRoute<HomeScreenDestination>() == true

    val koin = getKoin()
    val providers = remember(koin) { koin.getAll<NavGraphProvider>() }

    val resource by languageManager.currentResources.collectAsState()

    LaunchedEffect(key1 = Unit) {
        languageManager.setSystemLanguage()
    }

    // cmpose:backend
    // The sign-in gate (SessionGate.kt): nothing until the restored session is known, then the sign-in while
    // nobody is signed in, else the onboarding; an invite link opened while signed out shows after the sign-in
    // /cmpose:backend
    // cmpose:backend-off: val start: NavigationCommand.Destination = OnboardingDestination
    val start: NavigationCommand.Destination = rememberSessionStart(OnboardingDestination, navigationManager) ?: return

    AppTheme {
        CompositionLocalProvider(LocalStringResources provides resource) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    OfflineBanner(text = LocalStringResources.current.offlineBanner)
                    // cmpose:backend
                    // Debug builds: the local server does not answer (LocalServerBanner.kt); hidden while offline
                    LocalServerBanner()
                    // /cmpose:backend
                },
                containerColor = MaterialTheme.colorScheme.background,
                content = { paddingValues ->
                    NavHost(
                        modifier = Modifier
                            .padding(paddingValues)
                            .consumeWindowInsets(paddingValues),
                        navController = navController,
                        startDestination = start
                    ) {
                        providers.forEach {
                            it.registerGraph(provider = this)
                        }
                    }
                }
            )

            // Update available / required (core/update): at most one check a day
            AppUpdateEffect(texts = LocalStringResources.current.updateTexts())
        }
    }

    NavigationLaunchedEffect(
        navigationManager = navigationManager,
        navController = navController
    )

    // Passes ReviewPrompter's requests (a positive moment, "Rate the app") to the platform
    ReviewRequestEffect()

    // Purchases: the paywall once, after onboarding (feature/monetization)
    FirstLaunchPaywall(ready = onboardingDone)
}

private fun StringResourcesUiModel.updateTexts() = UpdateTexts(
    availableTitle = updateAvailableTitle,
    availableMessage = updateAvailableMessage,
    update = updateNow,
    later = updateLater,
    requiredTitle = updateRequiredTitle,
    requiredMessage = updateRequiredMessage,
    downloaded = updateDownloaded,
    restart = updateRestart
)
