package com.kturker.multiplatform.feature.home.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.designsystem.component.ErrorContent
import com.kturker.multiplatform.core.designsystem.component.KtAsyncImage
import com.kturker.multiplatform.core.designsystem.component.KtIcon
import com.kturker.multiplatform.core.designsystem.component.KtText
import com.kturker.multiplatform.core.designsystem.icon.IcBack
import com.kturker.multiplatform.core.designsystem.icon.IcStar
import com.kturker.multiplatform.core.designsystem.icon.IcStarOutline
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.designsystem.theme.KtMotion
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette
import com.kturker.multiplatform.core.designsystem.theme.Spacing
import com.kturker.multiplatform.core.multilang.resource.errorMessage
import com.kturker.multiplatform.feature.home.domain.model.GithubUserModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreenRoute(viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreen(uiState = uiState, actions = viewModel)
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    actions: HomeActions
) {
    val strings = LocalStringResources.current

    // The back button stays on every state, so a failed load still has a way out
    Column(modifier = Modifier.fillMaxSize().padding(all = Spacing.lg)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KtIcon(
                modifier = Modifier.padding(all = Spacing.xs).size(size = 48.dp).clickable {
                    actions.onNavigateUp()
                }.padding(all = Spacing.md),
                imageVector = IcBack,
                contentDescription = strings.back,
                tint = LocalColorsPalette.current.tintPrimary
            )

            if (uiState.user != null) {
                // 48dp touch target, 24dp icon; the outer 4dp keeps the 56dp slot the back button has
                KtIcon(
                    modifier = Modifier.padding(all = Spacing.xs).size(size = 48.dp).clickable {
                        actions.onFavoriteClick()
                    }.padding(all = Spacing.md),
                    imageVector = if (uiState.isFavorite) IcStar else IcStarOutline,
                    contentDescription = if (uiState.isFavorite) strings.removeFavorite else strings.addFavorite,
                    tint = LocalColorsPalette.current.tintPrimary
                )
            }
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
                        HomePane.LOADING -> CircularProgressIndicator()

                        HomePane.ERROR -> state.error?.let { error ->
                            ErrorContent(
                                message = strings.errorMessage(error = error),
                                retryLabel = strings.retry,
                                onRetry = actions::onRetry
                            )
                        }

                        HomePane.USER -> state.user?.let { user -> GithubUserDetail(user = user) }

                        HomePane.NONE -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun GithubUserDetail(user: GithubUserModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KtAsyncImage(
            image = user.avatarUrl.orEmpty(),
            contentDescription = null,
            modifier = Modifier
                .size(size = 120.dp)
                .clip(shape = CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(height = Spacing.md))

        if (user.name.isNullOrBlank().not()) {
            KtText(
                text = user.name.orEmpty(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                color = LocalColorsPalette.current.textPrimary
            )
        }

        KtText(
            text = "@${user.login.orEmpty()}",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalColorsPalette.current.textSecondary
        )

        Spacer(Modifier.height(height = Spacing.lg))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val strings = LocalStringResources.current
            StatItem(count = user.followers, label = strings.statFollowers)
            StatItem(count = user.following, label = strings.statFollowing)
            StatItem(count = user.publicRepos, label = strings.statRepos)
            StatItem(count = user.publicGists, label = strings.statGists)
        }
    }
}

@Composable
private fun StatItem(
    count: Int?,
    label: String
) {
    if (count != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            KtText(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = LocalColorsPalette.current.textPrimary
            )
            KtText(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LocalColorsPalette.current.textSecondary
            )
        }
    }
}
