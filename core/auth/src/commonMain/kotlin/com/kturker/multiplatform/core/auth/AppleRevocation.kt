package com.kturker.multiplatform.core.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val log = Logger.withTag("Auth")

// The account is deleted whether or not this works, so an unresponsive service must not hold that up
private val revokeTimeout = 10.seconds

/**
 * Keeps the one token that can revoke the app's access to the user's Apple account. Firebase doesn't store it
 * and it is only handed out at sign-in, so account deletion signs in again and holds it until the delete is done.
 */
internal class AppleRevocation(private val revoke: suspend (String) -> Boolean) {

    private var held: String? = null

    /** Holds what this sign-in returned; an answer without a token also forgets an earlier one. */
    fun hold(token: SocialToken) {
        held = when (token) {
            is SocialToken.Apple -> loggedIfMissing(token.authorizationCode)
            is SocialToken.SignedInByPlatform -> loggedIfMissing(token.appleAccessToken)
            else -> null
        }
    }

    // Only the absence is logged, never a token
    private fun loggedIfMissing(token: String?): String? = token.also {
        if (it == null) log.w { "Apple answered without a token to revoke" }
    }

    fun drop() {
        held = null
    }

    /**
     * A social sign-in that holds its token only once Firebase accepted the sign-in and [holdIf], asked after it,
     * still agrees. Acceptance does not prove the Apple identity is this user's, so the caller passes the uid
     * check in [holdIf]. Any earlier token is forgotten first, so a failed attempt leaves none.
     */
    suspend fun signInHoldingToken(
        platformToken: suspend () -> SocialToken,
        holdIf: () -> Boolean = { true },
        signIn: suspend (SocialToken) -> AuthResult
    ): AuthResult {
        drop()
        return socialSignInResult(platformToken) { token ->
            signIn(token).also { result -> if (result == AuthResult.Success && holdIf()) hold(token) }
        }
    }

    // Emptied before the call, so a token is tried at most once. A failure or a timeout must not stop the deletion
    // it is part of, so any exception but the caller's cancellation ends here
    @Suppress("TooGenericExceptionCaught")
    suspend fun revokeHeld() {
        val token = held ?: return
        held = null
        try {
            val revoked = withTimeoutOrNull(revokeTimeout) { revoke(token) } == true
            if (!revoked) log.w { "Apple token revocation failed" }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            log.w { "Apple token revocation failed" }
        }
    }
}
