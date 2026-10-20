plugins {
    alias(libs.plugins.kmp.library.plugin)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.domain)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                // cmpose:backend
                // Bearer token for the backend client (BackendClient.kt)
                implementation(libs.ktor.client.auth)
                // /cmpose:backend
                implementation(libs.ktor.serialization.kotlinx.json)
                // Network images for Coil (core:image): Coil finds the Ktor fetcher on the classpath
                implementation(libs.coil.network.ktor3)
                // Network monitor: release APKs swap in networkmonitor-noop (androidApp); iOS gates it at runtime
                implementation(libs.networkMonitor.debug)
            }
        }

        commonTest.dependencies {
            implementation(libs.kermit.test)
            // MockEngine: BaseRepositoryTest (with or without the backend) and the backend client's tests
            implementation(libs.ktor.client.mock)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.android)

        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
// cmpose:backend

// The backend client (BackendClient.kt) keeps the network monitor, which stores calls through SQLiter and so
// the system SQLite. The app's link brings it in; this module's iOS test executable has to ask for it.
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().configureEach {
    binaries.withType<org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable>().configureEach {
        linkerOpts("-lsqlite3")
    }
}
// /cmpose:backend