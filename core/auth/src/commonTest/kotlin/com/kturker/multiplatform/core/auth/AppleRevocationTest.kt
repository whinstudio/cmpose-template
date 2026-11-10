package com.kturker.multiplatform.core.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppleRevocationTest {

    private val revoked = mutableListOf<String>()
    private val revocation = AppleRevocation { token ->
        revoked += token
        true
    }

    @Test
    fun theAppleCodeIsRevokedOnce() = runTest {
        revocation.hold(SocialToken.Apple("id", "nonce", "code"))

        revocation.revokeHeld()
        revocation.revokeHeld()

        assertEquals(listOf("code"), revoked)
    }

    @Test
    fun androidsAccessTokenIsRevoked() = runTest {
        revocation.hold(SocialToken.SignedInByPlatform("access"))

        revocation.revokeHeld()

        assertEquals(listOf("access"), revoked)
    }

    @Test
    fun nothingHeldRevokesNothing() = runTest {
        // Each answer without a token also forgets the one an earlier sign-in left
        listOf(
            SocialToken.Cancelled,
            SocialToken.Apple("id", "nonce", null),
            SocialToken.SignedInByPlatform(null)
        ).forEach { noToken ->
            revocation.hold(SocialToken.Apple("id", "nonce", "code"))
            revocation.hold(noToken)

            revocation.revokeHeld()
        }

        assertEquals(emptyList(), revoked)
    }

    @Test
    fun dropForgetsTheToken() = runTest {
        revocation.hold(SocialToken.Apple("id", "nonce", "code"))

        revocation.drop()
        revocation.revokeHeld()

        assertEquals(emptyList(), revoked)
    }

    @Test
    fun aFailedRevocationDoesNotThrow() = runTest {
        var refusedAttempts = 0
        val refused = AppleRevocation {
            refusedAttempts++
            false
        }
        refused.hold(SocialToken.Apple("id", "nonce", "code"))
        refused.revokeHeld()
        refused.revokeHeld()

        var throwingAttempts = 0
        val throwing = AppleRevocation {
            throwingAttempts++
            throw IllegalStateException("revocation failed")
        }
        throwing.hold(SocialToken.Apple("id", "nonce", "code"))
        throwing.revokeHeld()
        throwing.revokeHeld()

        assertEquals(1, refusedAttempts)
        assertEquals(1, throwingAttempts)
    }

    // Virtual time runs the timeout out; revokeHeld returning, without the timeout's exception, is the assertion
    @Test
    fun aRevocationThatNeverAnswersDoesNotStallTheDeletion() = runTest {
        val stuck = AppleRevocation { awaitCancellation() }
        stuck.hold(SocialToken.Apple("id", "nonce", "code"))

        stuck.revokeHeld()
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        val cancelled = AppleRevocation { throw CancellationException("screen left") }
        cancelled.hold(SocialToken.Apple("id", "nonce", "code"))

        assertFailsWith<CancellationException> { cancelled.revokeHeld() }
    }

    private val appleToken = SocialToken.Apple("id", "nonce", "code")

    @Test
    fun aSuccessfulSignInHoldsItsToken() = runTest {
        val result = revocation.signInHoldingToken({ appleToken }, signIn = { AuthResult.Success })
        revocation.revokeHeld()

        assertEquals(AuthResult.Success, result)
        assertEquals(listOf("code"), revoked)
    }

    @Test
    fun aSignInAsAnotherUserHoldsNoToken() = runTest {
        val result = revocation.signInHoldingToken({ appleToken }, holdIf = { false }, signIn = { AuthResult.Success })
        revocation.revokeHeld()

        assertEquals(AuthResult.Success, result)
        assertEquals(emptyList(), revoked)
    }

    @Test
    fun aSignInFirebaseRefusedHoldsNothing() = runTest {
        val refused = AuthResult.Failure(AuthFailure.UNAVAILABLE)

        val result = revocation.signInHoldingToken({ appleToken }, signIn = { refused })
        revocation.revokeHeld()

        assertEquals(refused, result)
        assertEquals(emptyList(), revoked)
    }

    @Test
    fun aCancelledSignInHoldsNothing() = runTest {
        val result = revocation.signInHoldingToken({ SocialToken.Cancelled }, signIn = { AuthResult.Cancelled })
        revocation.revokeHeld()

        assertEquals(AuthResult.Cancelled, result)
        assertEquals(emptyList(), revoked)
    }

    @Test
    fun aRefusedSignInForgetsTheEarlierToken() = runTest {
        revocation.signInHoldingToken({ appleToken }, signIn = { AuthResult.Success })

        revocation.signInHoldingToken({ appleToken }, signIn = { AuthResult.Failure(AuthFailure.UNKNOWN) })
        revocation.revokeHeld()

        assertEquals(emptyList(), revoked)
    }

    @Test
    fun aPlatformExceptionBecomesAFailureAndHoldsNothing() = runTest {
        val result = revocation.signInHoldingToken(
            { throw IllegalStateException("another web operation is pending") },
            signIn = { error("Firebase sign-in must not run") }
        )
        revocation.revokeHeld()

        assertEquals(AuthResult.Failure(AuthFailure.UNKNOWN), result)
        assertEquals(emptyList(), revoked)
    }
}
