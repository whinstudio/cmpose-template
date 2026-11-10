package com.kturker.multiplatform.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.designsystem.icon.IcError
import com.kturker.multiplatform.core.designsystem.theme.Spacing

/**
 * A failed load: [message] centred above a [retryLabel] button that calls [onRetry]. It takes plain
 * strings, so each screen picks its texts (the showcase: LocalStringResources.current.errorMessage).
 */
@Composable
fun ErrorContent(
    message: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(all = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        // Decorative: the message says what went wrong
        KtIcon(
            modifier = Modifier.size(48.dp),
            imageVector = IcError,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        KtText(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Button(onClick = onRetry) {
            KtText(text = retryLabel)
        }
    }
}
