package com.kturker.multiplatform.core.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

/**
 * The backend turned the app away as too old (426 `CLIENT_UPGRADE_REQUIRED`, core:auth's backend client raises it).
 * Once raised it stays raised until the app restarts: AppUpdateEffect then shows the "Update required" screen.
 */
@Single
class ForcedUpdateSignal {
    private val state = MutableStateFlow(false)

    val required: StateFlow<Boolean> = state.asStateFlow()

    fun raise() {
        state.value = true
    }
}
