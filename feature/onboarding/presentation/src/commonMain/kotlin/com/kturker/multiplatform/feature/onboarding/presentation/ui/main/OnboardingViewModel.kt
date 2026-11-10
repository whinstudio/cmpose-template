package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.onError
import com.kturker.multiplatform.core.domain.onLoading
import com.kturker.multiplatform.core.domain.onSuccess
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.presentation.CoreViewModel
import com.kturker.multiplatform.feature.home.contract.HomeScreenDestination
import com.kturker.multiplatform.feature.home.domain.model.FavoriteUserModel
import com.kturker.multiplatform.feature.home.domain.usecase.ObserveFavoritesUseCase
import com.kturker.multiplatform.feature.home.domain.usecase.ToggleFavoriteUseCase
import com.kturker.multiplatform.feature.onboarding.contract.SettingsDestination
import com.kturker.multiplatform.feature.onboarding.domain.model.GithubUserListModel
import com.kturker.multiplatform.feature.onboarding.domain.usecase.SearchGithubUsersUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Provided

/** How long typing has to pause before the query is searched. */
internal const val SEARCH_DEBOUNCE_MILLIS = 400L

@KoinViewModel
internal class OnboardingViewModel(
    @Provided private val navigationManager: NavigationManager,
    @Provided private val searchGithubUsers: SearchGithubUsersUseCase,
    @Provided private val observeFavorites: ObserveFavoritesUseCase,
    @Provided private val toggleFavorite: ToggleFavoriteUseCase,
    @Provided private val connectivityMonitor: ConnectivityMonitor
) : CoreViewModel(),
    OnboardingActions {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState>
        get() = _uiState

    // The pending or running search: a newer query, a retry or a cleared field cancels it
    private var searchJob: Job? = null

    init {
        observeFavorites()
            .onEach { favorites -> _uiState.update { it.copy(favorites = favorites, favoritesLoaded = true) } }
            .launchIn(viewModelScope)
        // Back online after a connection error: load again (an HTTP or parsing error is not retried)
        connectivityMonitor.reconnections()
            .onEach { if (_uiState.value.error == DataError.Network) onRetry() }
            .launchIn(viewModelScope)
    }

    override fun searchUser() {
        search(query = _uiState.value.query, debounceMillis = 0L)
    }

    override fun onQueryChanged(username: String) {
        _uiState.update { it.copy(query = username, error = null) }
        search(query = username, debounceMillis = SEARCH_DEBOUNCE_MILLIS)
    }

    override fun onRetry() {
        search(query = _uiState.value.query, debounceMillis = 0L)
    }

    override fun onNavigateToDetail(user: GithubUserListModel) {
        navigationManager.navigate(navigationCommand = HomeScreenDestination(githubUsername = user.login))
    }

    override fun onNavigateToSettings() {
        navigationManager.navigate(navigationCommand = SettingsDestination)
    }

    override fun onFavoriteClick(favorite: FavoriteUserModel) {
        navigationManager.navigate(navigationCommand = HomeScreenDestination(githubUsername = favorite.login))
    }

    override fun onRemoveFavorite(favorite: FavoriteUserModel) {
        viewModelScope.launch { toggleFavorite(user = favorite, isFavorite = true) }
    }

    private fun search(
        query: String,
        debounceMillis: Long
    ) {
        searchJob?.cancel()
        if (query.isBlank()) {
            searchJob = null
            _uiState.update { it.copy(users = emptyList(), isLoading = false, error = null, searchedQuery = null) }
            return
        }
        _uiState.update { it.copy(isLoading = false) }
        searchJob = viewModelScope.launch {
            delay(debounceMillis)
            _uiState.update { it.copy(isLoading = true, error = null) }
            safeFlowApiCall {
                searchGithubUsers.invoke(username = query)
            }.onSuccess { users ->
                _uiState.update { it.copy(users = users, searchedQuery = query) }
            }.onError { error ->
                _uiState.update { it.copy(error = error) }
            }.onLoading { isLoading ->
                _uiState.update { it.copy(isLoading = isLoading) }
            }.collect()
        }
    }
}
