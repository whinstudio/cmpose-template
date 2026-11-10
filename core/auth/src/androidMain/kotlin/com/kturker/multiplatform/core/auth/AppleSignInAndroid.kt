package com.kturker.multiplatform.core.auth

import android.app.Activity
import co.touchlab.kermit.Logger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

private val log = Logger.withTag("AppleSignIn")

// The user closed the browser tab Firebase opened for Apple's sign-in page
private const val WEB_CONTEXT_CANCELED = "ERROR_WEB_CONTEXT_CANCELED"

/**
 * Sign in with Apple on Android: Android has no native Apple sign-in, so Firebase runs Apple's web flow
 * in a Custom Tab and signs the user in itself when it returns.
 */
internal class AppleSignInAndroid {

    suspend fun signIn(activity: Activity): SocialToken = tokenFromWebFlow {
        FirebaseAuth.getInstance().startActivityForSignInWithProvider(activity, appleProvider()).await().credential
    }

    /** The web flow for the user already signed in: Firebase refuses another Apple ID and keeps them. */
    suspend fun reauthenticate(activity: Activity): SocialToken {
        val user = FirebaseAuth.getInstance().currentUser ?: return SocialToken.Failed(AuthFailure.UNKNOWN)
        return tokenFromWebFlow(alsoCancels = { reauthResultOf(it) == AuthResult.Cancelled }) {
            user.startActivityForReauthenticateWithProvider(activity, appleProvider()).await().credential
        }
    }

    private fun appleProvider(): OAuthProvider = OAuthProvider.newBuilder(APPLE_PROVIDER)
        .setScopes(listOf("email", "name"))
        .build()

    private suspend fun tokenFromWebFlow(
        alsoCancels: (errorCode: String) -> Boolean = { false },
        webFlow: suspend () -> AuthCredential?
    ): SocialToken = try {
        SocialToken.SignedInByPlatform((webFlow() as? OAuthCredential)?.accessToken)
    } catch (e: FirebaseAuthException) {
        if (e.errorCode == WEB_CONTEXT_CANCELED || alsoCancels(e.errorCode)) {
            SocialToken.Cancelled
        } else {
            log.w { "Apple sign-in refused: ${e.errorCode}" }
            SocialToken.Failed(authFailureOf(e.errorCode))
        }
    } catch (_: FirebaseNetworkException) {
        SocialToken.Failed(AuthFailure.NETWORK)
    }

    suspend fun revoke(token: String): Boolean {
        FirebaseAuth.getInstance().revokeAccessToken(token).await()
        return true
    }
}
