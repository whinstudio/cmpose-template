package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.feature.home.contract.HomeScreenDestination
import com.kturker.multiplatform.feature.home.domain.model.FavoriteUserModel
import com.kturker.multiplatform.feature.home.domain.repository.FavoritesRepository
import com.kturker.multiplatform.feature.home.domain.usecase.ObserveFavoritesUseCase
import com.kturker.multiplatform.feature.home.domain.usecase.ToggleFavoriteUseCase
import com.kturker.multiplatform.feature.onboarding.contract.SettingsDestination
import com.kturker.multiplatform.feature.onboarding.domain.model.GithubUserListModel
import com.kturker.multiplatform.feature.onboarding.domain.repository.GithubRepository
import com.kturker.multiplatform.feature.onboarding.domain.usecase.SearchGithubUsersUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

// viewModelScope runs on Dispatchers.Main, which the tests swap for a test dispatcher. setMain comes
// before runTest, so both share one virtual clock and advanceTimeBy times the debounce. The use case
// still switches to Dispatchers.IO: tests wait for the state they assert with first { }, and a search
// the test must hold open waits on a CompletableDeferred.
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val octocat = GithubUserListModel(
        avatarUrl = "https://avatars.githubusercontent.com/u/583231",
        id = 583231,
        login = "octocat"
    )
    private val octocatFavorite = FavoriteUserModel(
        login = "octocat",
        avatarUrl = "https://avatars.githubusercontent.com/u/583231",
        name = null
    )
    // The fake favorites flow emits at once, so every view model has its favorites loaded
    private val loaded = OnboardingUiState(favoritesLoaded = true)
    private val favoritesRepository = FakeFavoritesRepository()
    private val searchedNames = mutableListOf<String>()
    private val navigationManager = RecordingNavigationManager()
    private val createdViewModels = mutableListOf<OnboardingViewModel>()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    // Every view model's scope is cancelled and joined while Main is still the test dispatcher: a
    // load finishing on IO after the test body resumes its parent on Main, and after resetMain
    // that dispatch would hit the real (unmocked) Main
    @AfterTest
    fun tearDown() = runTest {
        createdViewModels.forEach { it.viewModelScope.coroutineContext.job.cancelAndJoin() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        connectivityMonitor: ConnectivityMonitor = FakeConnectivityMonitor(),
        onGetUsers: suspend (username: String) -> RestResult<List<GithubUserListModel>> = {
            RestResult.Success(listOf(octocat))
        }
    ) = OnboardingViewModel(
        navigationManager = navigationManager,
        searchGithubUsers = SearchGithubUsersUseCase(
            repository = FakeGithubRepository { username ->
                searchedNames += username
                onGetUsers(username)
            }
        ),
        observeFavorites = ObserveFavoritesUseCase(repository = favoritesRepository),
        toggleFavorite = ToggleFavoriteUseCase(repository = favoritesRepository),
        connectivityMonitor = connectivityMonitor
    ).also { createdViewModels += it }

    @Test
    fun typingUpdatesTheQuery() {
        val viewModel = createViewModel()

        viewModel.onQueryChanged(username = "octo")

        assertEquals(loaded.copy(query = "octo"), viewModel.uiState.value)
    }

    @Test
    fun typingSearchesTheLastQueryOnceTypingPauses() = runTest {
        val release = CompletableDeferred<Unit>()
        val viewModel = createViewModel {
            release.await()
            RestResult.Success(listOf(octocat))
        }

        viewModel.onQueryChanged(username = "o")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        viewModel.onQueryChanged(username = "oc")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS - 1)
        assertFalse(viewModel.uiState.value.isLoading)

        advanceTimeBy(1)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoading)

        release.complete(Unit)
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }
        assertEquals(loaded.copy(query = "oc", users = listOf(octocat), searchedQuery = "oc"), state)
        assertEquals(listOf("oc"), searchedNames)
    }

    @Test
    fun theSearchKeySearchesAtOnceAndDropsThePendingSearch() = runTest {
        val release = CompletableDeferred<Unit>()
        val viewModel = createViewModel {
            if (searchedNames.size == 1) {
                release.await()
                RestResult.Success(listOf(octocat))
            } else {
                awaitCancellation()
            }
        }

        viewModel.onQueryChanged(username = "octo")
        viewModel.searchUser()
        assertTrue(viewModel.uiState.value.isLoading)

        release.complete(Unit)
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)

        assertEquals(loaded.copy(query = "octo", users = listOf(octocat), searchedQuery = "octo"), state)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("octo"), searchedNames)
    }

    @Test
    fun typingThenClearingBeforeThePauseSendsNoRequest() = runTest {
        val viewModel = createViewModel()

        viewModel.onQueryChanged(username = "octo")
        viewModel.onQueryChanged(username = "")
        advanceUntilIdle()

        assertEquals(loaded, viewModel.uiState.value)
        assertEquals(emptyList<String>(), searchedNames)
    }

    @Test
    fun clearingTheQueryWhileSearchingCancelsTheSearchAndClearsTheList() = runTest {
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val viewModel = createViewModel {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }

        viewModel.onQueryChanged(username = "octo")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        started.await()
        viewModel.onQueryChanged(username = "  ")
        cancelled.await()
        advanceUntilIdle()

        assertEquals(loaded.copy(query = "  "), viewModel.uiState.value)
        assertEquals(listOf("octo"), searchedNames)
    }

    @Test
    fun aBlankQueryClearsTheErrorToo() = runTest {
        val viewModel = createViewModel { RestResult.Error(DataError.Network) }
        viewModel.onQueryChanged(username = "octo")
        viewModel.uiState.first { it.error != null && !it.isLoading }

        viewModel.onQueryChanged(username = "")

        assertEquals(loaded, viewModel.uiState.value)
    }

    @Test
    fun typingClearsTheErrorAtOnceAndStillSearchesAfterThePause() = runTest {
        val viewModel = createViewModel {
            if (searchedNames.size == 1) RestResult.Error(DataError.Network) else RestResult.Success(listOf(octocat))
        }
        viewModel.onQueryChanged(username = "octo")
        viewModel.uiState.first { it.error != null && !it.isLoading }

        viewModel.onQueryChanged(username = "x")

        assertNull(viewModel.uiState.value.error)
        assertEquals(listOf("octo"), searchedNames)

        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }
        assertEquals(loaded.copy(query = "x", users = listOf(octocat), searchedQuery = "x"), state)
        assertEquals(listOf("octo", "x"), searchedNames)
    }

    @Test
    fun aFailedSearchShowsItsReason() = runTest {
        val viewModel = createViewModel { RestResult.Error(DataError.Http(code = 403)) }

        viewModel.onQueryChanged(username = "octo")
        val state = viewModel.uiState.first { it.error != null && !it.isLoading }

        assertEquals(loaded.copy(query = "octo", error = DataError.Http(code = 403)), state)
    }

    @Test
    fun retrySearchesTheQueryAgainAndClearsTheError() = runTest {
        val viewModel = createViewModel {
            if (searchedNames.size == 1) RestResult.Error(DataError.Network) else RestResult.Success(listOf(octocat))
        }
        viewModel.onQueryChanged(username = "octo")
        viewModel.uiState.first { it.error != null && !it.isLoading }

        viewModel.onRetry()
        assertNull(viewModel.uiState.value.error)
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }

        assertEquals(loaded.copy(query = "octo", users = listOf(octocat), searchedQuery = "octo"), state)
        assertEquals(listOf("octo", "octo"), searchedNames)
    }

    @Test
    fun aConnectionErrorSearchesAgainWhenBackOnline() = runTest {
        val monitor = FakeConnectivityMonitor()
        val viewModel = createViewModel(connectivityMonitor = monitor) {
            if (searchedNames.size == 1) RestResult.Error(DataError.Network) else RestResult.Success(listOf(octocat))
        }
        viewModel.onQueryChanged(username = "octo")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        viewModel.uiState.first { it.error == DataError.Network }

        monitor.online.value = false
        monitor.online.value = true
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }

        assertEquals(loaded.copy(query = "octo", users = listOf(octocat), searchedQuery = "octo"), state)
        assertEquals(listOf("octo", "octo"), searchedNames)
    }

    // Only a connection error waits for the connection: an HTTP error would fail the same way again
    @Test
    fun anHttpErrorIsNotSearchedAgainWhenBackOnline() = runTest {
        val monitor = FakeConnectivityMonitor()
        val viewModel = createViewModel(connectivityMonitor = monitor) { RestResult.Error(DataError.Http(code = 403)) }
        viewModel.onQueryChanged(username = "octo")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS)
        viewModel.uiState.first { it.error != null && !it.isLoading }

        monitor.online.value = false
        monitor.online.value = true
        advanceUntilIdle()

        assertEquals(listOf("octo"), searchedNames)
    }

    @Test
    fun retryWhileSearchingCancelsTheRunningSearch() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val viewModel = createViewModel {
            if (searchedNames.size == 1) {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled.complete(Unit)
                }
            } else {
                RestResult.Success(listOf(octocat))
            }
        }

        viewModel.onQueryChanged(username = "octo")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        firstStarted.await()
        viewModel.onRetry()
        firstCancelled.await()
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }

        assertEquals(loaded.copy(query = "octo", users = listOf(octocat), searchedQuery = "octo"), state)
        assertEquals(listOf("octo", "octo"), searchedNames)
    }

    @Test
    fun aNewQueryCancelsTheSearchStillRunning() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val viewModel = createViewModel { username ->
            if (username == "octo") {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled.complete(Unit)
                }
            } else {
                RestResult.Success(listOf(octocat))
            }
        }

        viewModel.onQueryChanged(username = "octo")
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS + 1)
        firstStarted.await()
        viewModel.onQueryChanged(username = "octocat")
        firstCancelled.await()
        val state = viewModel.uiState.first { it.users.isNotEmpty() && !it.isLoading }

        assertEquals(loaded.copy(query = "octocat", users = listOf(octocat), searchedQuery = "octocat"), state)
        assertEquals(listOf("octo", "octocat"), searchedNames)
    }

    @Test
    fun anEmptyResultRecordsTheSearchedQuery() = runTest {
        val viewModel = createViewModel { RestResult.Success(emptyList()) }

        viewModel.onQueryChanged(username = "octo")
        val state = viewModel.uiState.first { it.searchedQuery != null && !it.isLoading }

        assertEquals(loaded.copy(query = "octo", searchedQuery = "octo"), state)
        assertEquals(OnboardingPane.NO_RESULTS, state.pane)
    }

    @Test
    fun clearingTheFieldForgetsTheSearchedQuery() = runTest {
        val viewModel = createViewModel { RestResult.Success(emptyList()) }
        viewModel.onQueryChanged(username = "octo")
        viewModel.uiState.first { it.searchedQuery != null && !it.isLoading }

        viewModel.onQueryChanged(username = "")

        assertNull(viewModel.uiState.value.searchedQuery)
        assertEquals(OnboardingPane.PROMPT, viewModel.uiState.value.pane)
    }

    @Test
    fun theFirstFavoritesEmissionMarksThemLoaded() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.first { it.favoritesLoaded }

        assertTrue(state.favorites.isEmpty())
        assertTrue(state.showsFavoritesEmpty)
    }

    @Test
    fun selectingAUserOpensItsDetail() {
        createViewModel().onNavigateToDetail(user = octocat)

        assertEquals(
            listOf<NavigationCommand>(HomeScreenDestination(githubUsername = "octocat")),
            navigationManager.commands
        )
    }

    @Test
    fun settingsOpensTheSettingsScreen() {
        createViewModel().onNavigateToSettings()

        assertEquals(listOf<NavigationCommand>(SettingsDestination), navigationManager.commands)
    }

    @Test
    fun favoritesAreListedLive() = runTest {
        val viewModel = createViewModel()

        favoritesRepository.favorites.value = listOf(octocatFavorite)
        assertEquals(listOf(octocatFavorite), viewModel.uiState.first { it.favorites.isNotEmpty() }.favorites)

        favoritesRepository.favorites.value = emptyList()
        assertTrue(viewModel.uiState.first { it.favorites.isEmpty() }.favorites.isEmpty())
    }

    @Test
    fun removingAFavoriteDropsIt() = runTest {
        favoritesRepository.favorites.value = listOf(octocatFavorite)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.favorites.isNotEmpty() }

        viewModel.onRemoveFavorite(favorite = octocatFavorite)
        viewModel.uiState.first { it.favorites.isEmpty() }

        assertTrue(favoritesRepository.favorites.value.isEmpty())
    }

    @Test
    fun selectingAFavoriteOpensItsProfile() {
        createViewModel().onFavoriteClick(favorite = octocatFavorite)

        assertEquals(
            listOf<NavigationCommand>(HomeScreenDestination(githubUsername = "octocat")),
            navigationManager.commands
        )
    }
}

private class FakeConnectivityMonitor(initial: Boolean = true) : ConnectivityMonitor {
    val online = MutableStateFlow(initial)
    override val isOnline: StateFlow<Boolean> = online
}

private class FakeFavoritesRepository : FavoritesRepository {
    val favorites = MutableStateFlow<List<FavoriteUserModel>>(emptyList())

    override fun observeFavorites(): Flow<List<FavoriteUserModel>> = favorites

    override fun observeIsFavorite(login: String): Flow<Boolean> =
        favorites.map { list -> list.any { it.login == login } }

    override suspend fun add(user: FavoriteUserModel) {
        favorites.value += user
    }

    override suspend fun remove(login: String) {
        favorites.value = favorites.value.filterNot { it.login == login }
    }
}

private class RecordingNavigationManager : NavigationManager {
    val commands = mutableListOf<NavigationCommand>()
    override val navigationCommandFlow: Flow<NavigationCommand> = emptyFlow()

    override fun navigate(navigationCommand: NavigationCommand) {
        commands += navigationCommand
    }
}

private class FakeGithubRepository(
    private val onGetUsers: suspend (username: String) -> RestResult<List<GithubUserListModel>>
) : GithubRepository {
    override suspend fun getUsers(username: String): RestResult<List<GithubUserListModel>> =
        onGetUsers(username)
}
