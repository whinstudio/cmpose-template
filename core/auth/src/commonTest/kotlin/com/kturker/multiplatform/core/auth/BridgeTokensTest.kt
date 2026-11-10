package com.kturker.multiplatform.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals

/** The iOS bridge answers with (token, extra, error) strings; these become social sign-in results. */
class BridgeTokensTest {

    @Test
    fun aGoogleTokenWithoutErrorIsAGoogleCredential() {
        assertEquals(SocialToken.Google("id", "access"), googleTokenOf("id", "access", null))
        assertEquals(SocialToken.Google("id", null), googleTokenOf("id", null, null))
    }

    @Test
    fun anAppleTokenNeedsItsRawNonce() {
        assertEquals(SocialToken.Apple("id", "nonce"), appleTokenOf("id", "nonce", null))
        assertEquals(SocialToken.Failed(AuthFailure.UNKNOWN), appleTokenOf("id", null, null))
    }

    @Test
    fun anAppleTokenCarriesItsAuthorizationCode() {
        assertEquals(SocialToken.Apple("id", "nonce", "code"), appleTokenOf("id", "nonce", null, "code"))
    }

    @Test
    fun cancelledIsCancelledOnBothProviders() {
        assertEquals(SocialToken.Cancelled, googleTokenOf(null, null, "cancelled"))
        assertEquals(SocialToken.Cancelled, appleTokenOf(null, null, "cancelled"))
    }

    @Test
    fun otherErrorsAndMissingTokensAreFailures() {
        assertEquals(SocialToken.Failed(AuthFailure.NETWORK), googleTokenOf(null, null, "network"))
        assertEquals(SocialToken.Failed(AuthFailure.UNKNOWN), googleTokenOf(null, null, "boom"))
        assertEquals(SocialToken.Failed(AuthFailure.UNKNOWN), googleTokenOf(null, null, null))
        assertEquals(SocialToken.Failed(AuthFailure.UNKNOWN), appleTokenOf("id", "nonce", "boom"))
    }
}
