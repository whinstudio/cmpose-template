package com.kturker.multiplatform.core.insights.di

import com.kturker.multiplatform.core.insights.Insights
import com.kturker.multiplatform.core.insights.platformInsights
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
@ComponentScan("com.kturker.multiplatform.core.insights")
class InsightsModule {

    @Single
    fun provideInsights(): Insights = platformInsights()
}
