package com.kturker.multiplatform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.domain.ConnectivityMonitor
import com.kturker.multiplatform.core.network.LocalServerStatus
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.getKoin

// The local server strip shows only while the device is online: offline, OfflineBanner says it all
internal fun showsLocalServerBanner(
    unreachable: Boolean,
    online: Boolean
): Boolean = unreachable && online

// Debug builds: a band, styled as OfflineBanner, while the backend client cannot reach the local server
// (LocalServerStatus). Release builds never mark it, so nothing is drawn there.
@Composable
internal fun LocalServerBanner() {
    val koin = getKoin()
    val status = koin.getOrNull<LocalServerStatus>() ?: return
    // No ConnectivityMonitor: treated as online
    val onlineFlow = remember(koin) { koin.getOrNull<ConnectivityMonitor>()?.isOnline ?: MutableStateFlow(true) }
    val unreachable by status.isUnreachable.collectAsState()
    val online by onlineFlow.collectAsState()
    AnimatedVisibility(visible = showsLocalServerBanner(unreachable = unreachable, online = online)) {
        Text(
            text = localServerHint(),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .statusBarsPadding()
                .padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
    }
}
