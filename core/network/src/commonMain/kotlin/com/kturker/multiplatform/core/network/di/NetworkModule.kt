package com.kturker.multiplatform.core.network.di

import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.network.ApiConfig
import com.kturker.multiplatform.core.network.connectivity.createConnectivityMonitor
import com.kturker.multiplatform.core.network.getPlatformEngine
import com.kturker.multiplatform.core.network.installNetworkMonitor
import com.kturker.multiplatform.core.network.installRequestLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

private const val TIME_OUT = 45_000L

@Module
@ComponentScan("com.kturker.multiplatform.core.network")
class NetworkModule {

    @Single
    fun providePlatformEngine(): HttpClientEngine = getPlatformEngine()

    @Single
    fun provideHttpClient(
        engine: HttpClientEngine,
        @Provided environment: BuildEnvironment
    ): HttpClient = HttpClient(engine) {
        expectSuccess = true

        defaultRequest {
            url(ApiConfig.baseUrl(environment))
        }

        install(HttpTimeout) {
            requestTimeoutMillis = TIME_OUT
        }

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }

        installRequestLogging(isDebug = environment.isDebug)

        installNetworkMonitor()
    }

    // Whether the device is online: the offline banner and the screens' retry (core/domain ConnectivityMonitor)
    @Single
    fun provideConnectivityMonitor(): ConnectivityMonitor = createConnectivityMonitor()
}
