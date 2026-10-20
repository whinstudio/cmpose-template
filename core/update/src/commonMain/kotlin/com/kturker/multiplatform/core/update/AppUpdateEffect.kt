package com.kturker.multiplatform.core.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.koinInject

// Call once in MainScreen: checks at most once a day (every launch while an update is required) and shows
// the platform's update flow, an "Update available" dialog (iOS) or the "Update required" screen. The screen
// also opens as soon as the backend answers 426 (signal), whatever the daily check decided.
@Composable
fun AppUpdateEffect(
    texts: UpdateTexts = UpdateTexts(),
    checker: UpdateChecker = koinInject(),
    signal: ForcedUpdateSignal = koinInject()
) {
    var decision by remember { mutableStateOf<UpdateDecision?>(null) }
    var downloaded by remember { mutableStateOf(false) }
    val updater = rememberPlatformUpdater(onDownloaded = { downloaded = true })
    val serverRequired by signal.required.collectAsState()
    LaunchedEffect(checker) {
        decision = checker.check()
        if (decision == UpdateDecision.None && updater.startRecommendedUpdate()) decision = null
    }
    // The App Store page for the screen's button (iOS); the screen shows before the lookup answers
    var serverStoreUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(serverRequired) {
        if (serverRequired) serverStoreUrl = checker.storeUrl()
    }
    if (serverRequired) {
        RequiredUpdateDialog(texts = texts, onUpdate = { updater.openStore(serverStoreUrl) })
        return
    }
    when (val current = decision) {
        is UpdateDecision.Required -> RequiredUpdateDialog(
            texts = texts,
            onUpdate = { updater.openStore(current.storeUrl) }
        )

        is UpdateDecision.Available -> UpdateAlertDialog(
            title = texts.availableTitle,
            message = texts.availableMessage,
            confirm = texts.update,
            dismiss = texts.later,
            onConfirm = {
                updater.openStore(current.storeUrl)
                decision = null
            },
            onDismiss = { decision = null }
        )

        else -> Unit
    }
    if (downloaded) {
        UpdateAlertDialog(
            title = texts.downloaded,
            message = null,
            confirm = texts.restart,
            dismiss = texts.later,
            onConfirm = { updater.completeUpdate() },
            onDismiss = { downloaded = false }
        )
    }
}

// Back and a tap outside do not close it: the app is unusable below the minimum version
@Composable
private fun RequiredUpdateDialog(
    texts: UpdateTexts,
    onUpdate: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        UpdateRequiredScreen(texts = texts, onUpdate = onUpdate)
    }
}

@Composable
private fun UpdateAlertDialog(
    title: String,
    message: String?,
    confirm: String,
    dismiss: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = if (message != null) {
            { Text(text = message) }
        } else {
            null
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = dismiss) } }
    )
}
