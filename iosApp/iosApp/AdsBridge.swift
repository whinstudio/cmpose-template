import AppTrackingTransparency
import GoogleMobileAds
import os
import Shared
import UIKit
import UserMessagingPlatform

// The Google Mobile Ads side of core:ads. Kotlin/Native cannot link the SDK, so Kotlin calls this
// class through the IosAdsBridge protocol (registered in iOSApp.init()). Every method runs on the
// main thread, and every callback is called on the main thread.
final class AdsBridge: NSObject, IosAdsBridge {
    private var isMobileAdsStarted = false

    // Each request takes a new number; cancelPending() moves it on, so a consent form or ad that
    // finishes loading for a cancelled request (the screen is gone) is not presented, and a form's
    // late result is not reported.
    private var currentRequest = 0

    // Loaded, waiting for Kotlin to open it on the resumed screen; cancelPending() drops it.
    private var loadedConsentForm: ConsentForm?
    private var loadedInterstitial: InterstitialAd?
    private var loadedRewarded: RewardedAd?

    // A full-screen ad and its delegate must stay alive while the ad is on screen.
    private var interstitialAd: InterstitialAd?
    private var rewardedAd: RewardedAd?
    private var presentation: PresentationDelegate?

    // A banner holds its delegate weakly; one shared delegate serves every banner.
    private let bannerDelegate = BannerLoadDelegate()

    // Consent loads here and is presented by presentConsent, which Kotlin calls only on the resumed ads
    // screen: the UMP consent form, then App Tracking Transparency, then the SDK (only once canRequestAds is
    // true). A failing step does not stop the flow: the next step runs and canRequestAds tells Kotlin whether
    // ads may be requested.
    func loadConsent(
        onReady: @escaping () -> Void,
        onComplete: @escaping (KotlinBoolean, KotlinBoolean) -> Void
    ) {
        let request = beginRequest()
        let parameters = RequestParameters()
        #if DEBUG
        // Debug builds force the EEA geography (the GDPR form) on simulators; a physical device also
        // needs its test device identifier (debugSettings.testDeviceIdentifiers). See README "Monetization".
        let debugSettings = DebugSettings()
        debugSettings.geography = .EEA
        parameters.debugSettings = debugSettings
        #endif

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.isCurrent(request) else { return }
                let consent = ConsentInformation.shared
                guard consent.consentStatus == .required, consent.formStatus == .available else {
                    self.finishLoadingConsent(onReady: onReady, onComplete: onComplete)
                    return
                }
                ConsentForm.load { [weak self] form, _ in
                    DispatchQueue.main.async {
                        guard let self, self.isCurrent(request) else { return }
                        self.loadedConsentForm = form
                        self.finishLoadingConsent(onReady: onReady, onComplete: onComplete)
                    }
                }
            }
        }
    }

    func presentConsent(onComplete: @escaping (KotlinBoolean, KotlinBoolean) -> Void) {
        // Reads currentRequest, not the number from loadConsent: Kotlin never calls present for a cancelled request.
        let request = currentRequest
        let form = loadedConsentForm
        loadedConsentForm = nil
        guard let form else {
            requestTracking(request, onComplete)
            return
        }
        form.present(from: AdsBridge.topViewController()) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.isCurrent(request) else { return }
                self.requestTracking(request, onComplete)
            }
        }
    }

    // The UMP privacy options form (preloaded by UMP once it is required), where the user changes
    // the consent given earlier. Ads follow the answer.
    func showPrivacyOptions(onComplete: @escaping (KotlinBoolean, KotlinBoolean) -> Void) {
        let request = beginRequest()
        ConsentForm.presentPrivacyOptionsForm(from: AdsBridge.topViewController()) { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.isCurrent(request) else { return }
                self.completeConsent(onComplete)
            }
        }
    }

    func makeBanner(adUnitId: String) -> UIView {
        let banner = BannerView(adSize: AdSizeBanner)
        banner.adUnitID = adUnitId
        // Lets UI tests (Maestro) find the banner by id.
        banner.accessibilityIdentifier = "BannerAd"
        banner.rootViewController = AdsBridge.topViewController()
        banner.delegate = bannerDelegate
        banner.load(Request())
        return banner
    }

    func loadInterstitial(
        adUnitId: String,
        onLoaded: @escaping () -> Void,
        onFailed: @escaping (String) -> Void
    ) {
        let request = beginRequest()
        InterstitialAd.load(with: adUnitId, request: Request()) { [weak self] ad, error in
            guard let self, self.isCurrent(request) else { return }
            guard let ad else {
                onFailed(error?.localizedDescription ?? "The interstitial ad did not load")
                return
            }
            self.loadedInterstitial = ad
            onLoaded()
        }
    }

    func presentInterstitial(onDismissed: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        guard let ad = loadedInterstitial else {
            onFailed("The interstitial ad is no longer loaded")
            return
        }
        loadedInterstitial = nil
        interstitialAd = ad
        ad.fullScreenContentDelegate = makePresentation(onDismissed: onDismissed, onFailed: onFailed)
        ad.present(from: AdsBridge.topViewController())
    }

    func loadRewarded(
        adUnitId: String,
        onLoaded: @escaping () -> Void,
        onFailed: @escaping (String) -> Void
    ) {
        let request = beginRequest()
        RewardedAd.load(with: adUnitId, request: Request()) { [weak self] ad, error in
            guard let self, self.isCurrent(request) else { return }
            guard let ad else {
                onFailed(error?.localizedDescription ?? "The rewarded ad did not load")
                return
            }
            self.loadedRewarded = ad
            onLoaded()
        }
    }

    func presentRewarded(
        onReward: @escaping (KotlinInt, String) -> Void,
        onDismissed: @escaping () -> Void,
        onFailed: @escaping (String) -> Void
    ) {
        guard let ad = loadedRewarded else {
            onFailed("The rewarded ad is no longer loaded")
            return
        }
        loadedRewarded = nil
        rewardedAd = ad
        ad.fullScreenContentDelegate = makePresentation(onDismissed: onDismissed, onFailed: onFailed)
        // The reward is known once the ad loads; read now, it survives the release of the ad on
        // dismissal. A reward reported after the dismissal is awaited on the Kotlin side.
        let reward = ad.adReward
        ad.present(from: AdsBridge.topViewController()) {
            onReward(KotlinInt(int: reward.amount.int32Value), reward.type)
        }
    }

    // An ad already on screen is kept: its delegate releases it once the user closes it.
    func cancelPending() {
        currentRequest += 1
        loadedConsentForm = nil
        loadedInterstitial = nil
        loadedRewarded = nil
    }

    private func beginRequest() -> Int {
        currentRequest += 1
        return currentRequest
    }

    private func isCurrent(_ request: Int) -> Bool {
        request == currentRequest
    }

    // A loaded form or an unanswered ATT prompt waits for presentConsent; otherwise there is nothing to ask.
    private func finishLoadingConsent(
        onReady: () -> Void,
        onComplete: (KotlinBoolean, KotlinBoolean) -> Void
    ) {
        if loadedConsentForm != nil || ATTrackingManager.trackingAuthorizationStatus == .notDetermined {
            onReady()
        } else {
            completeConsent(onComplete)
        }
    }

    // The ATT prompt shows only while the user has not answered it; afterwards the call returns at once.
    private func requestTracking(_ request: Int, _ onComplete: @escaping (KotlinBoolean, KotlinBoolean) -> Void) {
        ATTrackingManager.requestTrackingAuthorization { [weak self] _ in
            DispatchQueue.main.async {
                guard let self, self.isCurrent(request) else { return }
                self.completeConsent(onComplete)
            }
        }
    }

    // Reports canRequestAds and privacyOptionsRequired; the SDK starts once ads may be requested.
    private func completeConsent(_ onComplete: (KotlinBoolean, KotlinBoolean) -> Void) {
        let consent = ConsentInformation.shared
        if consent.canRequestAds {
            startMobileAdsOnce()
        }
        onComplete(
            KotlinBoolean(bool: consent.canRequestAds),
            KotlinBoolean(bool: consent.privacyOptionsRequirementStatus == .required)
        )
    }

    private func startMobileAdsOnce() {
        guard !isMobileAdsStarted else { return }
        isMobileAdsStarted = true
        MobileAds.shared.start(completionHandler: nil)
    }

    // The delegate releases the ad (and itself) once the ad is gone.
    private func makePresentation(
        onDismissed: @escaping () -> Void,
        onFailed: @escaping (String) -> Void
    ) -> PresentationDelegate {
        let delegate = PresentationDelegate(
            onDismissed: { [weak self] in
                self?.releaseFullScreenAd()
                onDismissed()
            },
            onFailed: { [weak self] message in
                self?.releaseFullScreenAd()
                onFailed(message)
            }
        )
        presentation = delegate
        return delegate
    }

    private func releaseFullScreenAd() {
        interstitialAd = nil
        rewardedAd = nil
        presentation = nil
    }

    // The view controller on top of the key window: the Compose view controller, or whatever it presents.
    private static func topViewController() -> UIViewController? {
        let window = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

// A failed load leaves the slot empty; the log says why.
private final class BannerLoadDelegate: NSObject, BannerViewDelegate {
    private let log = os.Logger(subsystem: Bundle.main.bundleIdentifier ?? "app", category: "Ads")

    func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
        let code = (error as NSError).code
        log.warning("Banner failed to load: \(code, privacy: .public) \(error.localizedDescription, privacy: .public)")
    }
}

private final class PresentationDelegate: NSObject, FullScreenContentDelegate {
    private let onDismissed: () -> Void
    private let onFailed: (String) -> Void

    init(onDismissed: @escaping () -> Void, onFailed: @escaping (String) -> Void) {
        self.onDismissed = onDismissed
        self.onFailed = onFailed
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        onDismissed()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        onFailed(error.localizedDescription)
    }
}
