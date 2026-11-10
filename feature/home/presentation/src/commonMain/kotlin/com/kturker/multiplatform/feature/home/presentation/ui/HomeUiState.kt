package com.kturker.multiplatform.feature.home.presentation.ui

import com.kturker.multiplatform.core.domain.DataError
import com.kturker.multiplatform.feature.home.domain.model.GithubUserModel

internal data class HomeUiState(
    val user: GithubUserModel? = null,
    val isLoading: Boolean = false,
    val error: DataError? = null,
    val isFavorite: Boolean = false
) {
    /** What the area below the top bar shows. */
    val pane: HomePane
        get() = when {
            isLoading -> HomePane.LOADING
            error != null -> HomePane.ERROR
            user != null -> HomePane.USER
            else -> HomePane.NONE
        }
}

internal enum class HomePane { LOADING, ERROR, USER, NONE }
