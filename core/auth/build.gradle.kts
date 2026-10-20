plugins {
    alias(libs.plugins.kmp.library.plugin)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            // TokenSource, and the backend's HttpClient that AuthModule binds (BACKEND_CLIENT)
            implementation(projects.core.network)
            // The backend client's X-Client-Version (the app's version) and its 426 (the forced-update screen)
            implementation(projects.core.about)
            implementation(projects.core.update)
            implementation(libs.ktor.client.core)
            // The backend client's network images (BackendImageLoaderCustomizer, core:image's ImageLoader)
            implementation(projects.core.image)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kotlinx.coroutines.core)
            // GitLive's Firebase Auth: a common API over the Android SDK and the iOS SDK. On iOS the
            // Firebase SDK itself is not a dependency: the app links FirebaseAuth through SPM.
            implementation(libs.gitlive.firebase.auth)
        }
        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.play.services)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
        }
    }
}

// GitLive's iOS klibs ask the linker for the Firebase frameworks (FirebaseCore, FirebaseAuth), which only
// the Xcode app has (SPM): an iOS test executable cannot link ("framework 'FirebaseCore' not found"). This
// module's tests run on the JVM instead (testAndroidHostTest).
tasks.matching { it.name.startsWith("linkDebugTestIos") || it.name.matches(Regex("ios\\w*Test")) }.configureEach {
    enabled = false
}
