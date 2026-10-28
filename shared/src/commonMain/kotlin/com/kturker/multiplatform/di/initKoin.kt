@file:Suppress("Filename")

package com.kturker.multiplatform.di

import co.touchlab.kermit.Logger
import com.kturker.multiplatform.core.about.di.AboutModule
import com.kturker.multiplatform.core.auth.di.AuthModule
import com.kturker.multiplatform.core.database.di.DatabaseModule
import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.kturker.multiplatform.core.insights.di.InsightsModule
import com.kturker.multiplatform.core.insights.installInsightsLogging
import com.kturker.multiplatform.core.logging.configureLogging
import com.kturker.multiplatform.core.multilang.di.MultiLangModule
import com.kturker.multiplatform.core.navigation.NavigationModule
import com.kturker.multiplatform.core.network.di.NetworkModule
import com.kturker.multiplatform.core.purchases.di.PurchasesModule
import com.kturker.multiplatform.core.review.di.ReviewModule
import com.kturker.multiplatform.core.room.di.RoomModule
import com.kturker.multiplatform.core.update.di.UpdateModule
import com.kturker.multiplatform.feature.account.presentation.di.AccountModule
import com.kturker.multiplatform.feature.home.data.di.HomeDataModule
import com.kturker.multiplatform.feature.home.domain.di.HomeDomainModule
import com.kturker.multiplatform.feature.home.presentation.di.HomePresentationModule
import com.kturker.multiplatform.feature.monetization.presentation.di.MonetizationPresentationModule
import com.kturker.multiplatform.feature.onboarding.data.di.OnboardingDataModule
import com.kturker.multiplatform.feature.onboarding.domain.di.OnboardingDomainModule
import com.kturker.multiplatform.feature.onboarding.presentation.di.OnboardingPresentationModule
import com.kturker.multiplatform.feature.sharing.presentation.di.SharingModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.mp.KoinPlatform
import org.koin.core.module.Module as KoinModule

// The platform entry points start Koin: Android's Application, iOS's startKoinAtLaunch().
// Logging is set up first, so a class Koin creates can already log.
fun initKoin(
    isDebug: Boolean,
    config: KoinAppDeclaration? = null
) {
    configureLogging(isDebug = isDebug)
    startKoin {
        config?.invoke(this)
        modules(module { single { BuildEnvironment(isDebug) } })
        modules(appModules())
    }
    installInsightsLogging(KoinPlatform.getKoin().get())
    Logger.withTag("App").i { "Koin started (debug build: $isDebug)" }
}

// The Koin modules the app starts with. KoinGraphTest (shared/src/androidHostTest) fails when
// a class needs a type that none of them defines.
fun appModules(): List<KoinModule> = buildList {
    add(NavigationModule().module)
    add(AboutModule().module)
    add(OnboardingPresentationModule().module)
    add(OnboardingDataModule().module)
    add(OnboardingDomainModule().module)
    add(NetworkModule().module)
    // cmpose:backend
    add(AuthModule().module)
    add(AccountModule().module)
    add(SharingModule().module)
    // /cmpose:backend
    add(CmpModule().module)
    add(HomeDataModule().module)
    add(HomeDomainModule().module)
    add(HomePresentationModule().module)
    add(DatabaseModule().module)
    add(RoomModule().module)
    add(MultiLangModule().module)
    add(ReviewModule().module)
    add(UpdateModule().module)
    add(PurchasesModule().module)
    add(InsightsModule().module)
    add(MonetizationPresentationModule().module)
}

@Module
@ComponentScan("com.kturker.multiplatform")
class CmpModule
