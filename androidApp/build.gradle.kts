plugins {
    alias(libs.plugins.multiplatform.application.plugin)
    // cmpose:firebase
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    // /cmpose:firebase
}

// cmpose:firebase
// Firebase app config (sign-in with the backend, Crashlytics and Analytics with insights): the google-services plugin
// reads google-services.json into resources (default_web_client_id for Google sign-in, the Firebase app ID and API
// key). The file is per developer and git-ignored; without it the plugins are not applied, the app still builds,
// sign-in reports Unavailable and insights stay off. Crashlytics' plugin needs the file too (it writes the build ID
// the SDK checks at start, and uploads the R8 mapping of release builds).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// /cmpose:firebase
dependencies {
    implementation(projects.shared)
    // cmpose:backend
    // App.kt installs core:auth's CurrentActivity before Koin starts
    implementation(projects.core.auth)
    // /cmpose:backend
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
}

// Network monitor: release builds get the no-op artifact, so the real monitor
// (its manifest, call database and notifications) ships only in debug builds.
// The cmpose generator deletes this whole block (first comment line through the closing brace).
configurations.matching { it.isCanBeResolved && (it.name.startsWith("release") || it.name.contains("Release")) }.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.kturker:networkmonitor"))
            .using(module("com.kturker:networkmonitor-noop:${libs.versions.networkMonitor.get()}"))
    }
}
