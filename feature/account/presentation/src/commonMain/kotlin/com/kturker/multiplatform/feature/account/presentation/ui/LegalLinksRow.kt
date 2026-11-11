package com.kturker.multiplatform.feature.account.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kturker.multiplatform.core.about.LegalLink
import com.kturker.multiplatform.core.about.LegalLinkKind
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources

/**
 * The privacy policy and terms (core/about LegalUrls.kt), which the stores ask for where the account is made and
 * deleted. Nothing is shown while no URL is set. The labels are the app's settings texts, already translated.
 */
@Composable
internal fun LegalLinksRow(
    links: List<LegalLink>,
    onOpen: (url: String) -> Unit
) {
    if (links.isEmpty()) return
    val strings = LocalStringResources.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        links.forEach { link ->
            TextButton(onClick = { onOpen(link.url) }) {
                Text(
                    when (link.kind) {
                        LegalLinkKind.PrivacyPolicy -> strings.settingsPrivacyPolicy
                        LegalLinkKind.Terms -> strings.settingsTerms
                    }
                )
            }
        }
    }
}
