package com.kturker.multiplatform.feature.sharing.presentation.ui.members

import com.kturker.multiplatform.core.api.Member

/**
 * The name the members list shows: the display name, else the (verified) email, else [unnamed]. An email and password
 * sign-up has neither until it sets a name, and its row must not be blank.
 */
internal fun Member.shownName(unnamed: String): String =
    displayName?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() } ?: unnamed
