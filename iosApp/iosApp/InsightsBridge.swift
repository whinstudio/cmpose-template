import FirebaseAnalytics
import FirebaseCrashlytics
import Shared

// The Firebase side of core:insights (Crashlytics + Analytics). Kotlin/Native cannot link the SDK, so Kotlin calls
// this class through the IosInsightsBridge protocol (registered in iOSApp.init()). Without GoogleService-Info.plist
// Firebase is not configured and Kotlin makes no call here.
final class InsightsBridge: NSObject, IosInsightsBridge {
    let isConfigured: Bool

    init(firebaseConfigured: Bool) {
        isConfigured = firebaseConfigured
    }

    func logEvent(name: String, params: [String: Any]) {
        Analytics.logEvent(name, parameters: params.isEmpty ? nil : params)
    }

    func setUserId(id: String?) {
        // Crashlytics takes no nil: an empty id clears it
        Crashlytics.crashlytics().setUserID(id ?? "")
        Analytics.setUserID(id)
    }

    func recordNonFatal(name: String, message: String?, stack: [String]) {
        let model = ExceptionModel(name: name, reason: message ?? "")
        model.stackTrace = stack.map { StackFrame(symbol: $0, file: "", line: 0) }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func setCollectionEnabled(enabled: Bool) {
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
        Analytics.setAnalyticsCollectionEnabled(enabled)
    }
}
