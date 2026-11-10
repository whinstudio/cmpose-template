package com.kturker.multiplatform.core.auth

import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.mp.KoinPlatform.getKoin
import kotlin.coroutines.resume

/** iOS: implemented in Swift (GoogleSignIn, AuthenticationServices) and handed to Koin by registerSignInBridge. */
interface IosSignInBridge {
    /** True when the app found GoogleService-Info.plist and called `FirebaseApp.configure()`. */
    val firebaseConfigured: Boolean

    fun google(completion: (idToken: String?, accessToken: String?, error: String?) -> Unit)
    fun apple(
        completion: (idToken: String?, rawNonce: String?, error: String?, authorizationCode: String?) -> Unit
    )
    fun revokeApple(
        authorizationCode: String,
        completion: (error: String?) -> Unit
    )
}

internal class IosSocialSignIn(private val bridge: IosSignInBridge) : SocialSignIn {

    override suspend fun google(): SocialToken = suspendCancellableCoroutine { continuation ->
        bridge.google { idToken, accessToken, error ->
            if (continuation.isActive) continuation.resume(googleTokenOf(idToken, accessToken, error))
        }
    }

    override suspend fun apple(): SocialToken = suspendCancellableCoroutine { continuation ->
        bridge.apple { idToken, rawNonce, error, authorizationCode ->
            if (continuation.isActive) continuation.resume(appleTokenOf(idToken, rawNonce, error, authorizationCode))
        }
    }

    override suspend fun appleForCurrentUser(): SocialToken = apple()

    override suspend fun revokeApple(token: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            bridge.revokeApple(token) { error ->
                if (continuation.isActive) continuation.resume(error == null)
            }
        }
}

internal actual fun platformSocialSignIn(): SocialSignIn = IosSocialSignIn(bridge = getKoin().get())
