package com.kturker.multiplatform.core.insights.di

import com.kturker.multiplatform.core.insights.Insights
import com.kturker.multiplatform.core.insights.consent.InsightsConsentController
import com.kturker.multiplatform.core.insights.consent.InsightsConsentStore
import com.kturker.multiplatform.core.insights.consent.platformConsentStore
import com.kturker.multiplatform.core.insights.platformInsights
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.kturker.multiplatform.core.insights")
class InsightsModule {

    @Single
    fun provideInsights(): Insights = platformInsights()

    @Single
    fun provideConsentStore(): InsightsConsentStore = platformConsentStore()

    // Created when Koin starts, so the saved answers reach Firebase at launch (Android) without a line in the app's
    // own code: a project without insights has no such line to remove. iOS applies them again once the Swift bridge
    // is registered (registerInsightsBridge); before that the call goes nowhere.
    @Single(createdAtStart = true)
    fun provideConsentController(
        store: InsightsConsentStore,
        insights: Insights
    ): InsightsConsentController {
        val controller = InsightsConsentController(store, insights)
        controller.apply()
        return controller
    }
}
