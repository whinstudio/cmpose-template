package com.kturker.multiplatform.feature.home.presentation.ui

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.designsystem.component.ErrorContent
import com.kturker.multiplatform.core.designsystem.component.KtAsyncImage
import com.kturker.multiplatform.core.designsystem.component.KtIcon
import com.kturker.multiplatform.core.designsystem.component.KtText
import com.kturker.multiplatform.core.designsystem.icon.IcBack
import com.kturker.multiplatform.core.designsystem.icon.IcStar
import com.kturker.multiplatform.core.designsystem.icon.IcStarOutline
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.designsystem.theme.LocalColorsPalette
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
    Column(modifier = Modifier.fillMaxSize().padding(all = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KtIcon(
                modifier = Modifier.padding(all = 16.dp).size(size = 24.dp).clickable {
                    actions.onNavigateUp()
                },
                imageVector = IcBack,
                contentDescription = strings.back,
                tint = LocalColorsPalette.current.tintBlack
            )

            if (uiState.user != null) {
                // 48dp touch target, 24dp icon; the outer 4dp keeps the 56dp slot the back button has
                KtIcon(
                    modifier = Modifier.padding(all = 4.dp).size(size = 48.dp).clickable {
                        actions.onFavoriteClick()
                    }.padding(all = 12.dp),
                    imageVector = if (uiState.isFavorite) IcStar else IcStarOutline,
                    contentDescription = if (uiState.isFavorite) strings.removeFavorite else strings.addFavorite,
                    tint = LocalColorsPalette.current.tintBlack
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(weight = 1f),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()

                uiState.error != null -> ErrorContent(
                    message = strings.errorMessage(error = uiState.error),
                    retryLabel = strings.retry,
                    onRetry = actions::onRetry
                )

                uiState.user != null -> GithubUserDetail(user = uiState.user)
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

        Spacer(Modifier.height(height = 12.dp))

        if (user.name.isNullOrBlank().not()) {
            KtText(
                text = user.name.orEmpty(),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 28.sp,
                color = LocalColorsPalette.current.textBlack
            )
        }

        KtText(
            text = "@${user.login.orEmpty()}",
            fontSize = 14.sp,
            color = LocalColorsPalette.current.textGray
        )

        Spacer(Modifier.height(height = 16.dp))

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
                fontWeight = FontWeight.Bold,
                color = LocalColorsPalette.current.textBlack
            )
            KtText(
                text = label,
                color = LocalColorsPalette.current.textGray
            )
        }
    }
}
