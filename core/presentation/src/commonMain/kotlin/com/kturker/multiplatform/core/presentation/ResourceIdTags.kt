package com.kturker.multiplatform.core.presentation

import androidx.compose.ui.Modifier

/**
 * Makes the test tags under this node visible to UI automation (Maestro's `id:` selector). Android exposes test tags
 * as resource ids only below a node that asks for it; iOS exposes them anyway, so there it is a no-op. The app's root
 * Scaffold (MainScreen) already sets it; a dialog is its own window, so a dialog with test tags sets it too.
 */
expect fun Modifier.testTagsAsResourceId(): Modifier
