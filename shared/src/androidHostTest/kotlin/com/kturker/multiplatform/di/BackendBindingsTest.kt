@file:OptIn(KoinInternalApi::class)

package com.kturker.multiplatform.di

import com.kturker.multiplatform.core.about.AppInfo
import com.kturker.multiplatform.core.about.AppVersion
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.domain.BuildEnvironment
import com.kturker.multiplatform.core.network.BACKEND_CLIENT
import com.kturker.multiplatform.core.network.TokenSource
import io.ktor.client.HttpClient
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.module.flatten
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * KoinGraphTest does not read qualifiers or provider parameters, so the backend's bindings are checked here:
 * the named HttpClient that core:image and the backend features inject, and the auth service that gives it
 * its token.
 */
class BackendBindingsTest {

    private val definitions = flatten(appModules()).flatMap { it.mappings.values }.map { it.beanDefinition }.distinct()

    @Test
    fun theBackendClientIsBoundUnderItsName() {
        val clients = definitions.filter { it.primaryType == HttpClient::class }

        assertEquals(1, clients.count { it.qualifier == named(BACKEND_CLIENT) })
        // The app's other client, without a token, stays unnamed
        assertEquals(1, clients.count { it.qualifier == null })
    }

    @Test
    fun theAuthServiceIsTheTokenSource() {
        val auth = definitions.single { it.primaryType == AuthService::class }

        assertTrue(TokenSource::class in auth.secondaryTypes)
    }

    // Resolved, not only declared: its engine, the build type, the app's version and the token source all come from
    // the graph. The token source and the app info are stand-ins here: the real ones ask Android for a Context,
    // which a host test has not.
    @Test
    fun theBackendClientResolvesFromTheAppModules() {
        val tokens = object : TokenSource {
            override suspend fun token(forceRefresh: Boolean): String? = null
        }
        val appInfo = object : AppInfo {
            override val version = AppVersion(name = "1.0.0", code = "1")
            override val legalLinks = emptyList<LegalLink>()
        }
        val koin = koinApplication {
            allowOverride(true)
            modules(appModules())
            modules(
                module {
                    single { BuildEnvironment(isDebug = false) }
                    single<TokenSource> { tokens }
                    single<AppInfo> { appInfo }
                }
            )
        }.koin

        val client = koin.getOrNull<HttpClient>(named(BACKEND_CLIENT))

        assertNotNull(client)
        client.close()
    }
}
