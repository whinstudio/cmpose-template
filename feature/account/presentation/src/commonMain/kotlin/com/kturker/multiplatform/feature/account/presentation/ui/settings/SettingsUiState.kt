package com.kturker.multiplatform.feature.account.presentation.ui.settings

import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.feature.account.presentation.ui.UiError

internal data class SettingsUiState(
    val email: String? = null,
    val confirmingDelete: Boolean = false,
    /**
     * Spaces the user owns and shares with others (GET /spaces), which the deletion takes away from their members;
     * null while loading or when the server could not be asked.
     */
    val sharedSpacesToDelete: Int? = null,
    /** The shared-space count is still on its way; the deletion cannot be confirmed until it arrives or fails. */
    val loadingSharedSpaces: Boolean = false,
    val busy: Boolean = false,
    val needsReauth: Boolean = false,
    val error: UiError? = null,
    /** The privacy policy and terms below the deletion; empty while their URLs are (core/about LegalUrls.kt). */
    val legalLinks: List<LegalLink> = emptyList()
)
