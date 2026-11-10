package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.feature.onboarding.domain.model.GithubUserListModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingUiStateTest {

    private val user = GithubUserListModel(avatarUrl = "", id = 1, login = "octocat")

    @Test
    fun aBlankFieldAsksForASearch() = assertEquals(OnboardingPane.PROMPT, OnboardingUiState().pane)

    @Test
    fun typingBeforeTheSearchRunsShowsLoadingNotNoResults() = assertEquals(
        OnboardingPane.LOADING,
        OnboardingUiState(query = "oc", isLoading = false, searchedQuery = null).pane
    )

    @Test
    fun aFinishedEmptySearchSaysSo() = assertEquals(
        OnboardingPane.NO_RESULTS,
        OnboardingUiState(query = "oc", searchedQuery = "oc").pane
    )

    @Test
    fun aNewQueryAfterNoResultsWaitsAgain() = assertEquals(
        OnboardingPane.LOADING,
        OnboardingUiState(query = "oct", searchedQuery = "oc").pane
    )

    @Test
    fun anErrorWinsOverEverything() = assertEquals(
        OnboardingPane.ERROR,
        OnboardingUiState(query = "oc", users = listOf(user), error = DataError.Network).pane
    )

    @Test
    fun aRefillKeepsTheResultsPane() {
        assertEquals(
            OnboardingPane.RESULTS,
            OnboardingUiState(query = "oc", users = listOf(user), isLoading = true).pane
        )
        assertEquals(OnboardingPane.RESULTS, OnboardingUiState(query = "oct", users = listOf(user)).pane)
    }

    @Test
    fun favoritesEmptyWaitsForTheFirstEmission() {
        assertFalse(OnboardingUiState().showsFavoritesEmpty)
        assertTrue(OnboardingUiState(favoritesLoaded = true).showsFavoritesEmpty)
    }

    @Test
    fun favoritesEmptyStepsAsideWhileSearching() =
        assertFalse(OnboardingUiState(query = "oc", favoritesLoaded = true).showsFavoritesEmpty)
}
