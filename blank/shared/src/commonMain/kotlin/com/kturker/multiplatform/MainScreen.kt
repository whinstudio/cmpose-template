package com.kturker.multiplatform

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kturker.multiplatform.core.image.ImageLoaderSetup
// cmpose:insights
import com.kturker.multiplatform.core.insights.consent.ui.InsightsConsentDialog
// /cmpose:insights
import com.kturker.multiplatform.core.navigation.FeatureEntry
import com.kturker.multiplatform.core.navigation.NavGraphProvider
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.ReviewRequestEffect
import com.kturker.multiplatform.core.update.AppUpdateEffect
import com.kturker.multiplatform.feature.monetization.presentation.paywall.FirstLaunchPaywall
import org.koin.compose.getKoin
import org.koin.compose.koinInject

// The app's root, inside the app theme (AppTheme.kt): a guide screen listing the feature modules, then each module's
// screens. The app starts on GuideDestination (GuideDestination.kt), which the guide is registered under below.
// When you are ready, replace GuideScreen with your own home screen: register it under GuideDestination in place of
// the guide, then delete GuideScreen.kt.
// cmpose:backend
// From your home screen, leave a way to AccountSettingsDestination (account deletion, the privacy links): the stores
// require it.
// /cmpose:backend
// cmpose:insights
// Keep the privacy choices too: the About dialog shows InsightsConsentSettings (core/insights: usage data and crash
// reports); put it in your own settings, so the user can change both answers.
// /cmpose:insights
@Composable
fun MainScreen(navigationManager: NavigationManager = koinInject()) {
    // Coil's one image loader (core/image): caches, and Kermit logs in debug builds
    ImageLoaderSetup()

    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current

    val koin = getKoin()
    val providers = remember(koin) { koin.getAll<NavGraphProvider>() }
    // cmpose:backend-off: val entries = remember(koin) { koin.getAll<FeatureEntry>().sortedBy { it.title } }
    val entries = remember(koin) { koin.getAll<FeatureEntry>().filter { it.visible }.sortedBy { it.title } }

    // cmpose:backend
    // The sign-in gate (SessionGate.kt): nothing until the restored session is known, then the sign-in while
    // nobody is signed in, else the guide; an invite link opened while signed out shows after the sign-in
    // /cmpose:backend
    // cmpose:backend-off: val start: NavigationCommand.Destination = GuideDestination
    val start: NavigationCommand.Destination = rememberSessionStart(GuideDestination, navigationManager) ?: return

    AppTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                OfflineBanner(text = "No connection")
                // cmpose:backend
                // Debug builds: the local server does not answer (LocalServerBanner.kt); hidden while offline
                LocalServerBanner()
                // /cmpose:backend
            },
            content = { paddingValues ->
                NavHost(
                    modifier = Modifier
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues),
                    navController = navController,
                    startDestination = start
                ) {
                    composable<GuideDestination> {
                        GuideScreen(
                            entries = entries,
                            onEntryClick = { entry -> navigationManager.navigate(entry.destination) },
                            onDocumentationClick = {
                                uriHandler.openUri("https://whinstudio.github.io/cmpose/guide.html")
                            }
                        )
                    }
                    providers.forEach {
                        it.registerGraph(provider = this)
                    }
                }
            }
        )

        // Update available / required (core/update): once a day at most; inside the theme for its dialog
        AppUpdateEffect()
        // FirstLaunchPaywall (purchases): the paywall once, at the first launch (feature/monetization)
        FirstLaunchPaywall()
        // cmpose:insights
        // Analytics stays off until the user agrees: asked at launch until answered (core/insights)
        InsightsConsentDialog()
        // /cmpose:insights
    }

    NavigationLaunchedEffect(
        navigationManager = navigationManager,
        navController = navController
    )

    // Passes ReviewPrompter's requests (a positive moment, "Rate the app") to the platform
    ReviewRequestEffect()
}
