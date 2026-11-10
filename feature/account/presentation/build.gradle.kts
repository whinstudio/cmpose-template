plugins {
    alias(libs.plugins.feature.presentation.plugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.account.contract)
            // SelectedSpace: reloaded once the profile sync has renamed the personal space
            implementation(projects.feature.sharing.contract)
            // Space and Profile, the paths of GET /spaces, PUT /me/profile and DELETE /me
            implementation(projects.core.api)
            implementation(projects.core.auth)
            // The backend's HttpClient (BACKEND_CLIENT), BaseRepository, forgetToken()
            implementation(projects.core.network)
            // DataStore: a sign-up's name waiting for Firebase
            implementation(projects.core.database)
            implementation(libs.ktor.client.core)
            // NavigationBackHandler: back on the sign-up form returns to the sign-in form
            implementation(libs.navigationevent.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.network)
            implementation(libs.ktor.client.mock)
        }
    }
}

// GitLive's iOS klibs (through core:auth) ask the linker for the Firebase frameworks, which only the Xcode
// app has (SPM): an iOS test executable cannot link ("framework 'FirebaseCore' not found"). This module's
// tests run on the JVM instead (testAndroidHostTest).
tasks.matching { it.name.startsWith("linkDebugTestIos") || it.name.matches(Regex("ios\\w*Test")) }.configureEach {
    enabled = false
}
