package com.kturker.multiplatform

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.image.ImageLoaderSetup
import com.kturker.multiplatform.core.multilang.language.LanguageManager
import com.kturker.multiplatform.core.multilang.resource.StringResourcesUiModel
import com.kturker.multiplatform.core.navigation.FeatureEntry
import com.kturker.multiplatform.core.navigation.NavGraphProvider
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.ReviewRequestEffect
import com.kturker.multiplatform.core.update.AppUpdateEffect
import com.kturker.multiplatform.core.update.UpdateTexts
import com.kturker.multiplatform.feature.monetization.presentation.paywall.FirstLaunchPaywall
import org.koin.compose.getKoin
import org.koin.compose.koinInject

/**
 * The app's root, inside the app theme (AppTheme.kt): a guide screen listing the feature modules,
 * then each module's screens.
 * When you are ready, change startDestination to your own first screen and delete GuideScreen.kt.
 */
@Composable
fun MainScreen(
    languageManager: LanguageManager = koinInject(),
    navigationManager: NavigationManager = koinInject()
) {
    // Coil's one image loader (core/image): caches, and Kermit logs in debug builds
    ImageLoaderSetup()

    val navController = rememberNavController()
    val uriHandler = LocalUriHandler.current

    val koin = getKoin()
    val providers = remember(koin) { koin.getAll<NavGraphProvider>() }
    // cmpose:backend-off: val entries = remember(koin) { koin.getAll<FeatureEntry>().sortedBy { it.title } }
    val entries = remember(koin) { koin.getAll<FeatureEntry>().filter { it.visible }.sortedBy { it.title } }

    // The app's strings (LocalStringResources.current.<key>): the device language, or the saved choice
    val resources by languageManager.currentResources.collectAsState()

    LaunchedEffect(key1 = Unit) {
        languageManager.setSystemLanguage()
    }

    // cmpose:backend
    // The sign-in gate (SessionGate.kt): nothing until the restored session is known, then the sign-in while
    // nobody is signed in, else the guide; an invite link opened while signed out shows after the sign-in
    // /cmpose:backend
    // cmpose:backend-off: val start: NavigationCommand.Destination = GuideDestination
    val start: NavigationCommand.Destination = rememberSessionStart(GuideDestination, navigationManager) ?: return

    AppTheme {
        CompositionLocalProvider(LocalStringResources provides resources) {
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
            AppUpdateEffect(texts = LocalStringResources.current.updateTexts())
            // FirstLaunchPaywall (purchases): the paywall once, at the first launch (feature/monetization)
            FirstLaunchPaywall()
        }
    }

    NavigationLaunchedEffect(
        navigationManager = navigationManager,
        navController = navController
    )

    // Passes ReviewPrompter's requests (a positive moment, "Rate the app") to the platform
    ReviewRequestEffect()
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
