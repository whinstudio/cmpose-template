package com.kturker.multiplatform.core.auth

import android.app.Activity
import android.content.Context
import co.touchlab.kermit.Logger
import org.koin.mp.KoinPlatform.getKoin

internal class AndroidSocialSignIn(
    context: Context,
    private val currentActivity: () -> Activity? = { CurrentActivity.activity }
) : SocialSignIn {

    private val google = GoogleSignInAndroid(context)
    private val apple = AppleSignInAndroid()

    override suspend fun google(): SocialToken = withActivity { google.signIn(it) }

    override suspend fun apple(): SocialToken = withActivity { apple.signIn(it) }

    override suspend fun appleForCurrentUser(): SocialToken =
        withActivity { apple.reauthenticate(it) }

    override suspend fun revokeApple(token: String): Boolean = apple.revoke(token)

    private suspend fun withActivity(block: suspend (Activity) -> SocialToken): SocialToken {
        val activity = currentActivity() ?: run {
            Logger.withTag("Auth").e { "No resumed Activity: CurrentActivity.install was not called" }
            return SocialToken.Failed(AuthFailure.UNKNOWN)
        }
        return block(activity)
    }
}

internal actual fun platformSocialSignIn(): SocialSignIn =
    AndroidSocialSignIn(context = getKoin().get())
