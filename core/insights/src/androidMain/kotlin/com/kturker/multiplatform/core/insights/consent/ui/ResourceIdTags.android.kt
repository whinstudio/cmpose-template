package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

internal actual fun Modifier.testTagsAsResourceId(): Modifier =
    semantics { testTagsAsResourceId = true }
