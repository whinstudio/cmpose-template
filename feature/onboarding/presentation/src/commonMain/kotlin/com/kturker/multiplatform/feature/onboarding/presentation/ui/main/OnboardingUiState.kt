package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.feature.home.domain.model.FavoriteUserModel
import com.kturker.multiplatform.feature.onboarding.domain.model.GithubUserListModel

internal data class OnboardingUiState(
    val query: String = "",
    val users: List<GithubUserListModel> = listOf(),
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val favorites: List<FavoriteUserModel> = listOf(),
    // The query whose search last finished; null until one finishes or after the field is cleared
    val searchedQuery: String? = null,
    // False until the first favorites list arrives, so "no favorites" never flashes at launch
    val favoritesLoaded: Boolean = false
) {
    /** What the area below the search field shows. */
    val pane: OnboardingPane
        get() = when {
            error != null -> OnboardingPane.ERROR
            users.isNotEmpty() -> OnboardingPane.RESULTS
            query.isBlank() -> OnboardingPane.PROMPT
            isLoading || searchedQuery != query -> OnboardingPane.LOADING
            else -> OnboardingPane.NO_RESULTS
        }

    /** True when the favorites list is known to be empty and no search is typed. */
    val showsFavoritesEmpty: Boolean
        get() = favoritesLoaded && favorites.isEmpty() && query.isBlank()
}

internal enum class OnboardingPane { ERROR, LOADING, PROMPT, NO_RESULTS, RESULTS }
