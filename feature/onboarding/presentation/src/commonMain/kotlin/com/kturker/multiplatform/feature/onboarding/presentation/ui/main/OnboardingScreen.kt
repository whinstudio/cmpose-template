package com.kturker.multiplatform.feature.onboarding.presentation.ui.main

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.designsystem.component.ErrorContent
import com.kturker.multiplatform.core.designsystem.component.KtAsyncImage
import com.kturker.multiplatform.core.designsystem.component.KtIcon
import com.kturker.multiplatform.core.designsystem.component.KtSearchBar
import com.kturker.multiplatform.core.designsystem.component.KtText
import com.kturker.multiplatform.core.designsystem.icon.IcSettings
import com.kturker.multiplatform.core.designsystem.icon.IcStar
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette
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
                modifier = Modifier.padding(all = 16.dp).size(size = 32.dp),
                painter = painterResource(Res.drawable.compose_multiplatform),
                contentDescription = null
            )

            KtIcon(
                modifier = Modifier.padding(all = 16.dp).size(size = 32.dp).clickable {
                    actions.onNavigateToSettings()
                },
                imageVector = IcSettings,
                contentDescription = LocalStringResources.current.settingsTitle,
                tint = LocalColorsPalette.current.tintBlack
            )
        }

        KtSearchBar(
            text = uiState.query,
            placeHolderText = LocalStringResources.current.githubSearchHint,
            onValueChange = actions::onQueryChanged,
            onSearch = actions::searchUser
        )

        if (uiState.favorites.isNotEmpty()) {
            FavoritesSection(
                favorites = uiState.favorites,
                onFavoriteClick = actions::onFavoriteClick,
                onRemoveFavorite = actions::onRemoveFavorite
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f),
            contentAlignment = Alignment.Center
        ) {
            val strings = LocalStringResources.current
            when {
                uiState.error != null -> ErrorContent(
                    message = strings.errorMessage(error = uiState.error),
                    retryLabel = strings.retry,
                    onRetry = actions::onRetry
                )

                uiState.isLoading && uiState.users.isEmpty() -> CircularProgressIndicator()

                else -> UserList(users = uiState.users, onItemClick = actions::onNavigateToDetail)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        text = strings.favoritesTitle,
        fontWeight = FontWeight.Bold,
        color = LocalColorsPalette.current.textBlack
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = FAVORITES_MAX_HEIGHT.dp)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = LocalColorsPalette.current.textBlack
        )

        KtIcon(
            modifier = Modifier.size(size = 48.dp).clickable { onRemove(favorite) }.padding(all = 12.dp),
            imageVector = IcStar,
            contentDescription = LocalStringResources.current.removeFavorite,
            tint = LocalColorsPalette.current.tintBlack
        )
    }
}

@Composable
private fun UserList(
    users: List<GithubUserListModel>,
    onItemClick: (GithubUserListModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = LocalColorsPalette.current.textBlack
        )
    }
}

private const val FAVORITES_MAX_HEIGHT = 200
