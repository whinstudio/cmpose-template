package com.kturker.multiplatform.core.auth

// The iOS bridge's error strings (SignInBridge.swift): the user closed the sheet, or the device is offline
internal const val BRIDGE_CANCELLED = "cancelled"
internal const val BRIDGE_NETWORK = "network"

/** The iOS bridge's Google answer as a social sign-in result. */
internal fun googleTokenOf(
    idToken: String?,
    accessToken: String?,
    error: String?
): SocialToken = bridgeError(error)
    ?: idToken?.let { SocialToken.Google(it, accessToken) }
    ?: SocialToken.Failed(AuthFailure.UNKNOWN)

/** The iOS bridge's Apple answer; Firebase needs the raw nonce whose hash went into the request. */
internal fun appleTokenOf(
    idToken: String?,
    rawNonce: String?,
    error: String?,
    authorizationCode: String? = null
): SocialToken {
    bridgeError(error)?.let { return it }
    return if (idToken != null && rawNonce != null) {
        SocialToken.Apple(idToken, rawNonce, authorizationCode)
    } else {
        SocialToken.Failed(AuthFailure.UNKNOWN)
    }
}

private fun bridgeError(error: String?): SocialToken? = when (error) {
    null -> null
    BRIDGE_CANCELLED -> SocialToken.Cancelled
    BRIDGE_NETWORK -> SocialToken.Failed(AuthFailure.NETWORK)
    else -> SocialToken.Failed(AuthFailure.UNKNOWN)
}
