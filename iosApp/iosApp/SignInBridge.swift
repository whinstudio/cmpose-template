import AuthenticationServices
import CryptoKit
import FirebaseAuth
import FirebaseCore
import GoogleSignIn
import Shared
import UIKit

/// The Swift half of Google and Apple sign-in (core:auth's IosSignInBridge). Each call answers once with
/// (token, extra, error); error is "cancelled" when the user closed the sheet, "network" when offline.
/// Apple also answers with its authorization code, the one thing that can revoke the sign-in later.
final class SignInBridge: NSObject, IosSignInBridge {

    /// Whether iOSApp found GoogleService-Info.plist and configured Firebase; false turns sign-in off
    /// (core:auth's AuthState.Unavailable) instead of letting Firebase crash on first use.
    let firebaseConfigured: Bool

    private var appleFlow: AppleSignInFlow?

    init(firebaseConfigured: Bool) {
        self.firebaseConfigured = firebaseConfigured
        super.init()
    }

    func google(completion: @escaping (String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            // CLIENT_ID from GoogleService-Info.plist, which FirebaseApp.configure() read
            guard let clientID = FirebaseApp.app()?.options.clientID,
                  let presenter = Self.topViewController() else {
                completion(nil, nil, "unavailable")
                return
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error = error as NSError? {
                    completion(nil, nil, Self.errorName(error))
                    return
                }
                guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                    completion(nil, nil, "failed")
                    return
                }
                completion(idToken, user.accessToken.tokenString, nil)
            }
        }
    }

    func apple(completion: @escaping (String?, String?, String?, String?) -> Void) {
        DispatchQueue.main.async {
            let flow = AppleSignInFlow { [weak self] idToken, rawNonce, error, authorizationCode in
                self?.appleFlow = nil
                completion(idToken, rawNonce, error, authorizationCode)
            }
            // The controller holds its delegate weakly; the flow lives until it answers
            self.appleFlow = flow
            flow.start()
        }
    }

    func revokeApple(authorizationCode: String, completion: @escaping (String?) -> Void) {
        Auth.auth().revokeToken(withAuthorizationCode: authorizationCode) { error in
            completion(error == nil ? nil : "failed")
        }
    }

    private static func errorName(_ error: NSError) -> String {
        if error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue {
            return "cancelled"
        }
        return error.domain == NSURLErrorDomain ? "network" : "failed"
    }

    static func keyWindow() -> UIWindow? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }
    }

    static func topViewController() -> UIViewController? {
        var top = keyWindow()?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

/// One Sign in with Apple request: a random raw nonce whose SHA-256 goes into the request; Firebase
/// checks the identity token against the raw one.
private final class AppleSignInFlow: NSObject, ASAuthorizationControllerDelegate,
    ASAuthorizationControllerPresentationContextProviding {

    private let rawNonce = AppleSignInFlow.randomNonce()
    private let completion: (String?, String?, String?, String?) -> Void

    init(completion: @escaping (String?, String?, String?, String?) -> Void) {
        self.completion = completion
    }

    func start() {
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.email, .fullName]
        request.nonce = Self.sha256(rawNonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        controller.delegate = self
        controller.presentationContextProvider = self
        controller.performRequests()
    }

    func authorizationController(controller: ASAuthorizationController,
                                 didCompleteWithAuthorization authorization: ASAuthorization) {
        guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
              let tokenData = credential.identityToken,
              let idToken = String(data: tokenData, encoding: .utf8) else {
            completion(nil, nil, "failed", nil)
            return
        }
        // Only revocation needs the code: signing in without one still succeeds
        let authorizationCode = credential.authorizationCode.flatMap { String(data: $0, encoding: .utf8) }
        completion(idToken, rawNonce, nil, authorizationCode)
    }

    func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
        let cancelled = (error as? ASAuthorizationError)?.code == .canceled
        completion(nil, nil, cancelled ? "cancelled" : "failed", nil)
    }

    func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor {
        SignInBridge.keyWindow() ?? ASPresentationAnchor()
    }

    // 32 random bytes, hex-encoded
    private static func randomNonce() -> String {
        var bytes = [UInt8](repeating: 0, count: 32)
        let status = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        precondition(status == errSecSuccess, "No secure random bytes for the Apple sign-in nonce")
        return bytes.map { String(format: "%02x", $0) }.joined()
    }

    private static func sha256(_ input: String) -> String {
        SHA256.hash(data: Data(input.utf8)).map { String(format: "%02x", $0) }.joined()
    }
}
