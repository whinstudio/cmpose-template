// cmpose:firebase
import FirebaseCore
// /cmpose:firebase
// cmpose:backend
import GoogleSignIn
// /cmpose:backend
import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        // cmpose:firebase
        // Firebase app config (sign-in with the backend, Crashlytics and Analytics with insights):
        // GoogleService-Info.plist is per developer and git-ignored, and the build copies it into the app only
        // when it exists. Without it Firebase is not configured, sign-in reports Unavailable and insights stay
        // off; configure() would stop the app on a missing file. Configured here once, for both, and before Kotlin
        // starts: Crashlytics' terminate handler must be installed before the Kotlin runtime's, or an unhandled
        // Kotlin exception is reported as an anonymous C++ one.
        let firebaseConfigured = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil
        if firebaseConfigured { FirebaseApp.configure() }
        // /cmpose:firebase
        // Once per process; ContentView (and its view controller) is created per window.
        MainViewControllerKt.startKoinAtLaunch()
        // cmpose:backend
        // Koin resolves the sign-in bridge lazily, so registering it right after Koin starts is early enough.
        SignInRegistrationKt.registerSignInBridge(signIn: SignInBridge(firebaseConfigured: firebaseConfigured))
        // /cmpose:backend
        InsightsBridgeRegistrationKt.registerInsightsBridge(bridge: InsightsBridge(firebaseConfigured: firebaseConfigured))
        // Google Mobile Ads lives in Swift (AdsBridge.swift); Kotlin reaches it through this bridge.
        AdsBridgeRegistrationKt.registerAdsBridge(bridge: AdsBridge())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Deep links (multiplatform://…, Universal Links once set up): Kotlin opens the screen
                // cmpose:backend
                // Google Sign-In's return comes first; anything else is a deep link
                // /cmpose:backend
                // cmpose:backend-off: .onOpenURL { url in _ = DeepLinksKt.handleDeepLink(uri: url.absoluteString) }
                .onOpenURL { url in if !GIDSignIn.sharedInstance.handle(url) { _ = DeepLinksKt.handleDeepLink(uri: url.absoluteString) } }
        }
    }
}
