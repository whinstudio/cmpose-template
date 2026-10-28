plugins {
    alias(libs.plugins.kmp.library.plugin)
    alias(libs.plugins.compose.multiplatform.plugin)
    alias(libs.plugins.aboutLibraries)
}

kotlin {
    // The umbrella "Shared" framework consumed by the iOS app, and the only framework
    // in the project: the KMP library convention declares the iOS targets without one,
    // and every other module is linked into this one.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            export(projects.core.ads)
            // Swift implements core:insights' IosInsightsBridge and hands it to IosInsights.register
            export(projects.core.insights)
            // cmpose:backend
            // Swift implements core:auth's IosSignInBridge and hands it to registerSignInBridge
            export(projects.core.auth)
            // /cmpose:backend
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.database)
            implementation(projects.core.domain)
            implementation(projects.core.image)
            implementation(projects.core.multilang)
            implementation(projects.core.navigation)
            implementation(projects.core.about)
            implementation(projects.core.review)
            implementation(projects.core.update)
            implementation(projects.core.network)
            // cmpose:backend
            implementation(projects.core.auth)
            // /cmpose:backend
            implementation(projects.core.room)
            implementation(projects.feature.home.data)
            implementation(projects.feature.home.contract)
            implementation(projects.feature.home.domain)
            implementation(projects.feature.home.presentation)
            implementation(projects.feature.onboarding.data)
            implementation(projects.feature.onboarding.domain)
            implementation(projects.feature.onboarding.presentation)
            implementation(projects.feature.onboarding.contract)
            implementation(projects.core.designsystem)
            api(projects.core.ads)
            api(projects.core.insights)
            implementation(projects.core.purchases)
            implementation(projects.feature.monetization.contract)
            implementation(projects.feature.monetization.presentation)
            // cmpose:backend
            // The backend's sign-in gate (SessionGate.kt): sign-in and settings, spaces and invite links
            implementation(projects.feature.account.contract)
            implementation(projects.feature.account.presentation)
            implementation(projects.feature.sharing.contract)
            implementation(projects.feature.sharing.presentation)
            // /cmpose:backend
        }
        // cmpose:backend
        iosMain.dependencies {
            // Exported from the Shared framework (see the framework block above)
            api(projects.core.auth)
        }
        // /cmpose:backend
        // KoinGraphTest reads constructors through Kotlin reflection, which is JVM only.
        getByName("androidHostTest").dependencies {
            implementation(libs.kotlin.reflect)
            // cmpose:backend
            // BackendBindingsTest looks up the backend's HttpClient definition
            implementation(libs.ktor.client.core)
            // /cmpose:backend
        }
    }
}

// Open source licences (core/about's licences screen): generated from this module's dependencies, which
// are the app's, on every build; the JSON is git-ignored
aboutLibraries {
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = false
    }
    collect {
        fetchRemoteLicense = false
        fetchRemoteFunding = false
    }
}

// Every Compose task that reads composeResources runs after the export
val composeResourcesTaskPrefixes = listOf(
    "prepareComposeResourcesTaskFor",
    "copyNonXmlValueResourcesFor",
    "convertXmlValueResourcesFor"
)
tasks.matching { task -> composeResourcesTaskPrefixes.any { task.name.startsWith(it) } }.configureEach {
    dependsOn("exportLibraryDefinitions")
}
