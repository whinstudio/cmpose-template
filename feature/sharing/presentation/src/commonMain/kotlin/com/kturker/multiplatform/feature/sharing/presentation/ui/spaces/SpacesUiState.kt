package com.kturker.multiplatform.feature.sharing.presentation.ui.spaces

import com.kturker.multiplatform.core.api.Role
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError

internal data class SpacesUiState(
    /** The personal space first, then the server's order; empty until loaded. */
    val spaces: List<Space> = emptyList(),
    val selectedId: String? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val dialog: SpacesDialog? = null,
    val error: SharingError? = null
) {
    fun canRename(space: Space): Boolean = space.role == Role.OWNER

    // The personal space goes only with the account
    fun canDelete(space: Space): Boolean = space.role == Role.OWNER && !space.personal

    // The owner deletes a space instead; a member leaves even someone's personal space
    fun canLeave(space: Space): Boolean = space.role != Role.OWNER
}

internal sealed interface SpacesDialog {
    data object Create : SpacesDialog
    data class Rename(val space: Space) : SpacesDialog
    data class ConfirmDelete(val space: Space) : SpacesDialog
    data class ConfirmLeave(val space: Space) : SpacesDialog
}
