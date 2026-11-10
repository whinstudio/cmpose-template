package com.kturker.multiplatform.core.auth

/** Platform half of social sign-in: returns a Firebase credential source or a cancel/failure. */
interface SocialSignIn {
    suspend fun google(): SocialToken
    suspend fun apple(): SocialToken

    /** Apple once more for the user already signed in, whom the platform must not replace with another account. */
    suspend fun appleForCurrentUser(): SocialToken

    /** Revokes [token], the one the platform's Apple sign-in returned; false when it could not. */
    suspend fun revokeApple(token: String): Boolean
}

sealed interface SocialToken {
    data class Google(val idToken: String, val accessToken: String?) : SocialToken

    /** iOS Apple; [authorizationCode], when Apple gave one, is what can revoke the sign-in later. */
    data class Apple(val idToken: String, val rawNonce: String, val authorizationCode: String? = null) : SocialToken

    /** Android Apple: Firebase finished the web flow itself; [appleAccessToken] can revoke it later. */
    data class SignedInByPlatform(val appleAccessToken: String?) : SocialToken
    data object Cancelled : SocialToken
    data class Failed(val reason: AuthFailure) : SocialToken
}

/** Android: Credential Manager and Firebase's Apple web flow; iOS: the Swift [IosSignInBridge]. */
internal expect fun platformSocialSignIn(): SocialSignIn
