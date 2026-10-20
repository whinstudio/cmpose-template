package com.kturker.multiplatform.feature.sharing.presentation.data

/** The deep link path an invite opens: `<scheme>://invite/<token>` (SharingDeepLink). */
internal const val INVITE_PATH = "invite"

/** The link an owner shares; the app opens it on the invite's preview. */
fun inviteUrl(
    scheme: String,
    token: String
) = "$scheme://$INVITE_PATH/$token"

/**
 * Whether [token] can be an invite token: the server issues base64url ones (letters, digits, `-` and `_`). Anything
 * else, an empty token or one with a `/` in it, never reaches the server (which gets the token in the request body).
 */
internal fun isInviteToken(token: String): Boolean =
    token.isNotEmpty() && token.all { it.isBase64Url() }

private fun Char.isBase64Url(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_'
