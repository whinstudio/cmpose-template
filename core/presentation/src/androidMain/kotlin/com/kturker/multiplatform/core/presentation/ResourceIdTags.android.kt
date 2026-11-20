package com.kturker.multiplatform.core.presentation

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

actual fun Modifier.testTagsAsResourceId(): Modifier = semantics { testTagsAsResourceId = true }
