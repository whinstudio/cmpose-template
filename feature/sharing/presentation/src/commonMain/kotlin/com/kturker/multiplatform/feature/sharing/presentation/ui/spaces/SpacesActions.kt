package com.kturker.multiplatform.feature.sharing.presentation.ui.spaces

import com.kturker.multiplatform.core.api.Space

internal interface SpacesActions {
    fun onBack()
    fun retry()

    /** Hides an error that is not retried (a quota). */
    fun dismissError()
    fun select(spaceId: String)
    fun openMembers(spaceId: String)
    fun startCreate()
    fun startRename(space: Space)

    /** The name typed into the create or rename dialog. */
    fun submitName(name: String)
    fun requestDelete(space: Space)
    fun requestLeave(space: Space)

    /** Confirms the open delete or leave dialog. */
    fun confirm()
    fun dismissDialog()
}
