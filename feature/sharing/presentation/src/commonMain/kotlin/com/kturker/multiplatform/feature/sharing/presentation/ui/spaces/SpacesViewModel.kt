package com.kturker.multiplatform.feature.sharing.presentation.ui.spaces

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.auth.AuthService
import com.kturker.multiplatform.core.auth.SessionTokens
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.sharing.contract.MembersDestination
import com.kturker.multiplatform.feature.sharing.contract.SelectedSpace
import com.kturker.multiplatform.feature.sharing.presentation.data.SpacesRepository
import com.kturker.multiplatform.feature.sharing.presentation.ui.BackendFailures
import com.kturker.multiplatform.feature.sharing.presentation.ui.SharingError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/**
 * The user's spaces: switching the selected one, creating one, and renaming, deleting (owner) or leaving
 * (member) one. Each change reloads the list; deleting, leaving and renaming also refresh [SelectedSpace], which
 * falls back to the personal space when the selected one is gone. A selected space the list does not have
 * (joined through an invite meanwhile) reloads the list.
 */
@KoinViewModel
internal class SpacesViewModel(
    private val repository: SpacesRepository,
    @Provided private val auth: AuthService,
    @Provided private val tokens: SessionTokens,
    private val selectedSpace: SelectedSpace,
    @Provided private val navigationManager: NavigationManager
) : CoreViewModel(),
    SpacesActions {

    private val _state = MutableStateFlow(SpacesUiState())
    val state: StateFlow<SpacesUiState> = _state.asStateFlow()

    private val failures = BackendFailures(auth, tokens, selectedSpace)

    init {
        viewModelScope.launch {
            selectedSpace.current.collect { space ->
                _state.update { it.copy(selectedId = space?.id) }
                // A space joined over this screen (an accepted invite) is selected before this list has it
                val state = _state.value
                if (space != null && !state.loading && state.spaces.none { it.id == space.id }) load()
            }
        }
        viewModelScope.launch { load() }
    }

    override fun onBack() = navigationManager.navigate(NavigationCommand.NavigateUp)

    override fun retry() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch { load() }
    }

    override fun dismissError() = _state.update { it.copy(error = null) }

    override fun select(spaceId: String) {
        viewModelScope.launch { selectedSpace.select(spaceId) }
    }

    override fun openMembers(spaceId: String) =
        navigationManager.navigate(NavigationCommand.NavigateTo(MembersDestination(spaceId)))

    override fun startCreate() =
        _state.update { it.copy(dialog = SpacesDialog.Create, error = null) }

    override fun startRename(space: Space) =
        _state.update { it.copy(dialog = SpacesDialog.Rename(space), error = null) }

    override fun submitName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        when (val dialog = _state.value.dialog) {
            SpacesDialog.Create -> change(refreshSelection = false, quota = SharingError.SPACE_LIMIT) {
                repository.create(trimmed)
            }

            is SpacesDialog.Rename -> change(refreshSelection = true) { repository.rename(dialog.space.id, trimmed) }

            else -> Unit
        }
    }

    override fun requestDelete(space: Space) =
        _state.update { it.copy(dialog = SpacesDialog.ConfirmDelete(space), error = null) }

    override fun requestLeave(space: Space) =
        _state.update { it.copy(dialog = SpacesDialog.ConfirmLeave(space), error = null) }

    override fun confirm() {
        when (val dialog = _state.value.dialog) {
            is SpacesDialog.ConfirmDelete -> change(refreshSelection = true) { repository.delete(dialog.space.id) }

            // The backend knows who is asking: no user id goes with it
            is SpacesDialog.ConfirmLeave -> change(refreshSelection = true) { repository.leave(dialog.space.id) }

            else -> Unit
        }
    }

    override fun dismissDialog() = _state.update { it.copy(dialog = null) }

    // quota: what 409 QUOTA_EXCEEDED means for this call (BackendFailures.handle)
    private fun change(
        refreshSelection: Boolean,
        quota: SharingError = SharingError.UNKNOWN,
        call: suspend () -> RestResult<Any>
    ) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, dialog = null, error = null) }
        viewModelScope.launch {
            val result = call()
            if (result is RestResult.Error) {
                val error = failures.handle(result.error, quota)
                _state.update { it.copy(busy = false, error = error) }
                // A 404 (the space is gone) changes the list too
                if (error != null) load(keepError = true)
                return@launch
            }
            if (refreshSelection) selectedSpace.refresh()
            load()
            _state.update { it.copy(busy = false) }
        }
    }

    private suspend fun load(keepError: Boolean = false) {
        when (val result = repository.list()) {
            is RestResult.Success -> _state.update {
                it.copy(
                    spaces = result.result.sortedByDescending(Space::personal),
                    loading = false,
                    error = if (keepError) it.error else null
                )
            }

            is RestResult.Error -> {
                val error = failures.handle(result.error)
                _state.update { it.copy(loading = false, error = if (keepError) it.error else error) }
            }

            is RestResult.Loading -> Unit
        }
    }
}
