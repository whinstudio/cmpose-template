package com.kturker.multiplatform.core.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocialSignInResultTest {

    /** A platform half that throws what Credential Manager or Firebase's Apple web flow may throw. */
    private class ThrowingSocialSignIn(private val error: Exception) : SocialSignIn {
        override suspend fun google(): SocialToken = throw error
        override suspend fun apple(): SocialToken = throw error
        override suspend fun appleForCurrentUser(): SocialToken = throw error
        override suspend fun revokeApple(token: String): Boolean = throw error
    }

    private val unreachedSignIn: suspend (SocialToken) -> AuthResult = { error("Firebase sign-in must not run") }

    @Test
    fun aPlatformExceptionBecomesAFailureNotACrash() = runTest {
        val social = ThrowingSocialSignIn(IllegalStateException("another web operation is pending"))

        assertEquals(AuthResult.Failure(AuthFailure.UNKNOWN), socialSignInResult(social::google, unreachedSignIn))
        assertEquals(AuthResult.Failure(AuthFailure.UNKNOWN), socialSignInResult(social::apple, unreachedSignIn))
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        val social = ThrowingSocialSignIn(CancellationException("screen left"))

        assertFailsWith<CancellationException> { socialSignInResult(social::google, unreachedSignIn) }
    }

    @Test
    fun aPlatformTokenGoesOnToFirebase() = runTest {
        val social = object : SocialSignIn {
            override suspend fun google(): SocialToken = SocialToken.Google("id", null)
            override suspend fun apple(): SocialToken = SocialToken.Cancelled
            override suspend fun appleForCurrentUser(): SocialToken = SocialToken.Cancelled
            override suspend fun revokeApple(token: String): Boolean = false
        }
        val seen = mutableListOf<SocialToken>()

        val result = socialSignInResult(social::google) { token ->
            seen += token
            AuthResult.Success
        }

        assertEquals(AuthResult.Success, result)
        assertEquals(listOf<SocialToken>(SocialToken.Google("id", null)), seen)
    }
}
