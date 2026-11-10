package com.kturker.multiplatform.feature.home.presentation.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.core.domain.RestResult
import com.kturker.multiplatform.core.navigation.NavigationCommand
import com.kturker.multiplatform.core.navigation.NavigationManager
import com.kturker.multiplatform.core.review.ReviewCounterStore
import com.kturker.multiplatform.core.review.ReviewPrompter
import com.kturker.multiplatform.feature.home.domain.model.FavoriteUserModel
import com.kturker.multiplatform.feature.home.domain.model.GithubUserModel
import com.kturker.multiplatform.feature.home.domain.repository.FavoritesRepository
import com.kturker.multiplatform.feature.home.domain.repository.GithubRepository
import com.kturker.multiplatform.feature.home.domain.usecase.GetGithubUserDetailUseCase
import com.kturker.multiplatform.feature.home.domain.usecase.ObserveIsFavoriteUseCase
import com.kturker.multiplatform.feature.home.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

// viewModelScope runs on Dispatchers.Main, which the tests swap for a test dispatcher. The use case
// still switches to Dispatchers.IO, so each test waits for the state it asserts with first { };
// a load the test must hold open waits on a CompletableDeferred.
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val octocat = githubUser(login = "octocat")
    private val requestedNames = mutableListOf<String>()
    private val octocatFavorite = FavoriteUserModel(login = "octocat", avatarUrl = "", name = null)
    private val favoritesRepository = FakeFavoritesRepository()
    private val navigationManager = RecordingNavigationManager()
    private val reviewCounter = InMemoryReviewCounterStore()
    private val createdViewModels = mutableListOf<HomeViewModel>()

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

    // The route's argument as navigation stores it: under the property's name
    private fun createViewModel(
        connectivityMonitor: ConnectivityMonitor = FakeConnectivityMonitor(),
        onGetUserDetail: suspend (username: String) -> RestResult<GithubUserModel> = { RestResult.Success(octocat) }
    ) = HomeViewModel(
        savedStateHandle = SavedStateHandle(mapOf("githubUsername" to "octocat")),
        getGithubUserDetail = GetGithubUserDetailUseCase(
            repository = FakeGithubRepository { username ->
                requestedNames += username
                onGetUserDetail(username)
            }
        ),
        observeIsFavorite = ObserveIsFavoriteUseCase(repository = favoritesRepository),
        toggleFavorite = ToggleFavoriteUseCase(repository = favoritesRepository),
        navigationManager = navigationManager,
        reviewPrompter = ReviewPrompter(store = reviewCounter),
        connectivityMonitor = connectivityMonitor
    ).also { createdViewModels += it }

    @Test
    fun opensWithTheUserOfTheRoute() = runTest {
        val state = createViewModel().uiState.first { it.user != null && !it.isLoading }

        assertEquals(HomeUiState(user = octocat), state)
        assertEquals(listOf("octocat"), requestedNames)
    }

    @Test
    fun aFailedLoadShowsItsReasonInsteadOfAnEmptyProfile() = runTest {
        val viewModel = createViewModel { RestResult.Error(DataError.Http(code = 500)) }

        val state = viewModel.uiState.first { it.error != null && !it.isLoading }

        assertEquals(HomeUiState(error = DataError.Http(code = 500)), state)
    }

    @Test
    fun retryLoadsTheUserAgainAndClearsTheError() = runTest {
        val viewModel = createViewModel {
            if (requestedNames.size == 1) RestResult.Error(DataError.Network) else RestResult.Success(octocat)
        }
        viewModel.uiState.first { it.error != null && !it.isLoading }

        viewModel.onRetry()
        assertNull(viewModel.uiState.value.error)
        val state = viewModel.uiState.first { it.user != null && !it.isLoading }

        assertEquals(HomeUiState(user = octocat), state)
        assertEquals(listOf("octocat", "octocat"), requestedNames)
    }

    @Test
    fun aConnectionErrorLoadsAgainWhenBackOnline() = runTest {
        val monitor = FakeConnectivityMonitor()
        val viewModel = createViewModel(connectivityMonitor = monitor) {
            if (requestedNames.size == 1) RestResult.Error(DataError.Network) else RestResult.Success(octocat)
        }
        viewModel.uiState.first { it.error == DataError.Network }

        monitor.online.value = false
        monitor.online.value = true
        val state = viewModel.uiState.first { it.user != null }

        assertEquals(octocat, state.user)
        assertEquals(listOf("octocat", "octocat"), requestedNames)
    }

    // Only a connection error waits for the connection: an HTTP error would fail the same way again
    @Test
    fun anHttpErrorIsNotRetriedWhenBackOnline() = runTest {
        val monitor = FakeConnectivityMonitor()
        val viewModel = createViewModel(connectivityMonitor = monitor) { RestResult.Error(DataError.Http(code = 500)) }
        viewModel.uiState.first { it.error != null && !it.isLoading }

        monitor.online.value = false
        monitor.online.value = true

        assertEquals(DataError.Http(code = 500), viewModel.uiState.value.error)
        assertEquals(1, requestedNames.size)
    }

    @Test
    fun retryWhileLoadingCancelsTheRunningLoad() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val firstCancelled = CompletableDeferred<Unit>()
        val viewModel = createViewModel {
            if (requestedNames.size == 1) {
                firstStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    firstCancelled.complete(Unit)
                }
            } else {
                RestResult.Success(octocat)
            }
        }
        firstStarted.await()

        viewModel.onRetry()
        firstCancelled.await()
        val state = viewModel.uiState.first { it.user != null && !it.isLoading }

        assertEquals(HomeUiState(user = octocat), state)
        assertEquals(listOf("octocat", "octocat"), requestedNames)
    }

    // Back cancels viewModelScope: the running request stops and no error is shown
    @Test
    fun leavingTheScreenCancelsTheLoadWithoutAnError() = runTest {
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
        started.await()

        viewModel.viewModelScope.cancel()
        cancelled.await()

        assertNull(viewModel.uiState.value.error)
        assertNull(viewModel.uiState.value.user)
    }

    @Test
    fun backNavigatesUp() {
        createViewModel { awaitCancellation() }.onNavigateUp()

        assertEquals(listOf<NavigationCommand>(NavigationCommand.NavigateUp), navigationManager.commands)
    }

    @Test
    fun theStarShowsWhetherTheUserIsAFavorite() = runTest {
        favoritesRepository.favorites.value = listOf(octocatFavorite)

        val state = createViewModel().uiState.first { it.isFavorite }

        assertTrue(state.isFavorite)
    }

    @Test
    fun theStarAddsTheUserToFavorites() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.first { it.user != null && !it.isLoading }

        viewModel.onFavoriteClick()
        viewModel.uiState.first { it.isFavorite }

        assertEquals(listOf(octocatFavorite), favoritesRepository.favorites.value)
    }

    @Test
    fun theStarRemovesAFavorite() = runTest {
        favoritesRepository.favorites.value = listOf(octocatFavorite)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.user != null && it.isFavorite }

        viewModel.onFavoriteClick()
        viewModel.uiState.first { !it.isFavorite }

        assertTrue(favoritesRepository.favorites.value.isEmpty())
    }

    @Test
    fun addingAFavoriteIsAPositiveMoment() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.first { it.user != null && !it.isLoading }

        viewModel.onFavoriteClick()
        viewModel.uiState.first { it.isFavorite }

        assertEquals(1, reviewCounter.getInt(key = POSITIVE_EVENTS_KEY, default = 0))
    }

    @Test
    fun removingAFavoriteIsNotAPositiveMoment() = runTest {
        favoritesRepository.favorites.value = listOf(octocatFavorite)
        val viewModel = createViewModel()
        viewModel.uiState.first { it.user != null && it.isFavorite }

        viewModel.onFavoriteClick()
        viewModel.uiState.first { !it.isFavorite }

        assertEquals(0, reviewCounter.getInt(key = POSITIVE_EVENTS_KEY, default = 0))
    }

    // Same order as the screen used to check: loading, then error, then the user
    @Test
    fun paneFollowsTheHomeScreenOrder() {
        assertEquals(HomePane.LOADING, HomeUiState(isLoading = true, user = githubUser(login = "a")).pane)
        assertEquals(HomePane.ERROR, HomeUiState(error = DataError.Network, user = githubUser(login = "a")).pane)
        assertEquals(HomePane.USER, HomeUiState(user = githubUser(login = "a")).pane)
        assertEquals(HomePane.NONE, HomeUiState().pane)
    }
}

// ReviewPrompter's key, repeated on purpose: the count is kept on the device, so renaming the key
// would lose it and should break this test
private const val POSITIVE_EVENTS_KEY = "review.positiveEvents"

private class InMemoryReviewCounterStore : ReviewCounterStore {
    private val values = mutableMapOf<String, Int>()

    override fun getInt(
        key: String,
        default: Int
    ): Int = values[key] ?: default

    override fun putInt(
        key: String,
        value: Int
    ) {
        values[key] = value
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
    private val onGetUserDetail: suspend (username: String) -> RestResult<GithubUserModel>
) : GithubRepository {
    override suspend fun getUserDetail(username: String): RestResult<GithubUserModel> =
        onGetUserDetail(username)
}

private fun githubUser(login: String) = GithubUserModel(
    login = login,
    id = 1,
    nodeId = null,
    avatarUrl = null,
    gravatarId = null,
    url = null,
    htmlUrl = null,
    followersUrl = null,
    followingUrl = null,
    gistsUrl = null,
    starredUrl = null,
    subscriptionsUrl = null,
    organizationsUrl = null,
    reposUrl = null,
    eventsUrl = null,
    receivedEventsUrl = null,
    type = null,
    siteAdmin = null,
    name = null,
    company = null,
    blog = null,
    location = null,
    email = null,
    hireable = null,
    bio = null,
    twitterUsername = null,
    publicRepos = null,
    publicGists = null,
    followers = null,
    following = null,
    createdAt = null,
    updatedAt = null
)
