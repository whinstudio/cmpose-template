package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.designsystem.component.EmptyState
import com.kturker.multiplatform.core.designsystem.component.ErrorContent
import com.kturker.multiplatform.core.designsystem.component.KtAsyncImage
import com.kturker.multiplatform.core.designsystem.component.KtIcon
import com.kturker.multiplatform.core.designsystem.component.KtSearchBar
import com.kturker.multiplatform.core.designsystem.component.KtText
import com.kturker.multiplatform.core.designsystem.icon.IcSettings
import com.kturker.multiplatform.core.designsystem.icon.IcStar
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.designsystem.theme.KtMotion
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette
import com.kturker.multiplatform.core.designsystem.theme.Spacing
import com.kturker.multiplatform.core.multilang.resource.errorMessage
import com.kturker.multiplatform.feature.home.domain.model.FavoriteUserModel
import com.kturker.multiplatform.feature.onboarding.domain.model.GithubUserListModel
import com.kturker.multiplatform.onboarding.presentation.generated.resources.Res
import com.kturker.multiplatform.onboarding.presentation.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingScreenRoute(viewModel: OnboardingViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingScreen(
        uiState = uiState,
        actions = viewModel
    )
}

@Composable
private fun OnboardingScreen(
    uiState: OnboardingUiState,
    actions: OnboardingActions
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A Compose Multiplatform resource (Res.*) on the first screen: if resources stop
            // resolving on Android, the app fails at launch instead of on some later screen.
            Image(
                modifier = Modifier.padding(all = Spacing.lg).size(size = 32.dp),
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = null
            )

            KtIcon(
                modifier = Modifier.padding(all = Spacing.xs).size(size = 48.dp).clickable {
                    actions.onNavigateToSettings()
                }.padding(all = Spacing.md),
                imageVector = IcSettings,
                contentDescription = LocalStringResources.current.settingsTitle,
                tint = LocalColorsPalette.current.tintPrimary
            )
        }

        KtSearchBar(
            text = uiState.query,
            placeHolderText = LocalStringResources.current.githubSearchHint,
            onValueChange = actions::onQueryChanged,
            onSearch = actions::searchUser
        )

        val strings = LocalStringResources.current

        if (uiState.favorites.isNotEmpty()) {
            FavoritesSection(
                favorites = uiState.favorites,
                onFavoriteClick = actions::onFavoriteClick,
                onRemoveFavorite = actions::onRemoveFavorite
            )
        } else if (uiState.showsFavoritesEmpty) {
            EmptyState(
                title = strings.favoritesEmptyTitle,
                message = strings.favoritesEmptyMessage,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f),
            contentAlignment = Alignment.Center
        ) {
            // Crossfades only when the pane changes; the branches read state, not uiState
            AnimatedContent(
                targetState = uiState,
                contentKey = { it.pane },
                transitionSpec = {
                    fadeIn(KtMotion.base()) togetherWith
                        fadeOut(KtMotion.exit()) using null
                },
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
                label = "pane"
            ) { state ->
                // AnimatedContent hands its full size down as a minimum; this Box lets a pane keep its own size
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (state.pane) {
                        OnboardingPane.ERROR -> state.error?.let { error ->
                            ErrorContent(
                                message = strings.errorMessage(error = error),
                                retryLabel = strings.retry,
                                onRetry = actions::onRetry
                            )
                        }

                        OnboardingPane.LOADING -> CircularProgressIndicator()

                        OnboardingPane.PROMPT -> EmptyState(
                            title = strings.searchPromptTitle,
                            message = strings.searchPromptMessage,
                            modifier = Modifier.padding(all = Spacing.xl)
                        )

                        OnboardingPane.NO_RESULTS -> EmptyState(
                            title = strings.searchNoResultsTitle,
                            message = strings.searchNoResultsMessage,
                            modifier = Modifier.padding(all = Spacing.xl)
                        )

                        OnboardingPane.RESULTS -> UserList(
                            users = state.users,
                            onItemClick = actions::onNavigateToDetail
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesSection(
    favorites: List<FavoriteUserModel>,
    onFavoriteClick: (FavoriteUserModel) -> Unit,
    onRemoveFavorite: (FavoriteUserModel) -> Unit
) {
    val strings = LocalStringResources.current

    KtText(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        text = strings.favoritesTitle,
        style = MaterialTheme.typography.titleMedium,
        color = LocalColorsPalette.current.textPrimary
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = FAVORITES_MAX_HEIGHT.dp)
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(favorites, key = { it.login }) { favorite ->
            FavoriteRow(
                favorite = favorite,
                onClick = onFavoriteClick,
                onRemove = onRemoveFavorite
            )
        }
    }
}

@Composable
private fun FavoriteRow(
    favorite: FavoriteUserModel,
    onClick: (FavoriteUserModel) -> Unit,
    onRemove: (FavoriteUserModel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(favorite) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        KtAsyncImage(
            image = favorite.avatarUrl,
            contentDescription = favorite.login,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        KtText(
            modifier = Modifier.weight(weight = 1f),
            text = "@${favorite.login}",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalColorsPalette.current.textPrimary
        )

        KtIcon(
            modifier = Modifier.size(size = 48.dp).clickable { onRemove(favorite) }.padding(all = Spacing.md),
            imageVector = IcStar,
            contentDescription = LocalStringResources.current.removeFavorite,
            tint = LocalColorsPalette.current.tintPrimary
        )
    }
}

@Composable
private fun UserList(
    users: List<GithubUserListModel>,
    onItemClick: (GithubUserListModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        items(users) { item ->
            UserInfoCard(
                user = item,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun UserInfoCard(
    user: GithubUserListModel,
    onItemClick: (GithubUserListModel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            onItemClick.invoke(user)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        KtAsyncImage(
            image = user.avatarUrl,
            contentDescription = user.login,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )

        KtText(
            text = "@${user.login}",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalColorsPalette.current.textPrimary
        )
    }
}

private const val FAVORITES_MAX_HEIGHT = 200
