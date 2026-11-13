package com.kturker.multiplatform.core.insights.consent.ui

import androidx.compose.ui.Modifier

/**
 * Makes the test tags under this node visible to UI automation (Maestro's `id:` selector). Android exposes test tags
 * as resource ids only below a node that asks for it; iOS exposes them anyway, so there it is a no-op.
 */
internal expect fun Modifier.testTagsAsResourceId(): Modifier
