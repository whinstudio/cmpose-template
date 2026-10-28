package com.kturker.multiplatform.feature.sharing.presentation.ui.spaces

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kturker.multiplatform.core.api.Space
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.sharing
import com.kturker.multiplatform.feature.sharing.presentation.ui.label
import com.kturker.multiplatform.feature.sharing.presentation.ui.retryable
import com.kturker.multiplatform.feature.sharing.presentation.ui.sharingErrorText
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SpacesRoute(viewModel: SpacesViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SpacesScreen(state = state, actions = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpacesScreen(
    state: SpacesUiState,
    actions: SpacesActions
) {
    val strings = LocalStringResources.current.sharing
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.spacesTitle) },
                navigationIcon = { TextButton(onClick = actions::onBack) { Text(strings.back) } },
                actions = {
                    TextButton(onClick = actions::startCreate, enabled = !state.busy) { Text(strings.newSpace) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.loading || state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            state.error?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        sharingErrorText(error),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    if (error.retryable) {
                        TextButton(onClick = actions::retry) { Text(strings.retry) }
                    } else {
                        TextButton(onClick = actions::dismissError) { Text(strings.close) }
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(state.spaces, key = Space::id) { space ->
                    SpaceCard(
                        space = space,
                        selected = space.id == state.selectedId,
                        state = state,
                        actions = actions
                    )
                }
            }
        }
    }

    state.dialog?.let { SpacesDialogFor(dialog = it, actions = actions) }
}

@Composable
private fun SpaceCard(
    space: Space,
    selected: Boolean,
    state: SpacesUiState,
    actions: SpacesActions
) {
    val strings = LocalStringResources.current.sharing
    Card(modifier = Modifier.fillMaxWidth().clickable { actions.select(space.id) }) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = { actions.select(space.id) })
                Column(modifier = Modifier.weight(1f)) {
                    Text(space.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${space.role.label(strings)} · ${strings.membersTitle}: ${space.memberCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (selected) Text(strings.selected, style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { actions.openMembers(space.id) }) { Text(strings.membersTitle) }
                if (state.canRename(space)) {
                    TextButton(onClick = { actions.startRename(space) }, enabled = !state.busy) {
                        Text(strings.rename)
                    }
                }
                if (state.canDelete(space)) {
                    TextButton(
                        onClick = { actions.requestDelete(space) },
                        enabled = !state.busy,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(strings.delete)
                    }
                }
                if (state.canLeave(space)) {
                    TextButton(onClick = { actions.requestLeave(space) }, enabled = !state.busy) {
                        Text(strings.leave)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpacesDialogFor(
    dialog: SpacesDialog,
    actions: SpacesActions
) {
    val strings = LocalStringResources.current.sharing
    when (dialog) {
        SpacesDialog.Create -> NameDialog(title = strings.newSpace, initial = "", confirm = strings.create, actions)

        is SpacesDialog.Rename -> NameDialog(
            title = strings.rename,
            initial = dialog.space.name,
            confirm = strings.save,
            actions = actions
        )

        is SpacesDialog.ConfirmDelete -> ConfirmDialog(
            title = dialog.space.name,
            text = strings.deleteSpaceWarning,
            confirm = strings.delete,
            actions = actions
        )

        is SpacesDialog.ConfirmLeave -> ConfirmDialog(
            title = dialog.space.name,
            text = strings.leaveSpaceWarning,
            confirm = strings.leave,
            actions = actions
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    actions: SpacesActions
) {
    val strings = LocalStringResources.current.sharing
    var name by rememberSaveable(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(strings.spaceName) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { actions.submitName(name) }, enabled = name.isNotBlank()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = actions::dismissDialog) { Text(strings.cancel) } }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    actions: SpacesActions
) {
    val strings = LocalStringResources.current.sharing
    AlertDialog(
        onDismissRequest = actions::dismissDialog,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            OutlinedButton(
                onClick = actions::confirm,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = actions::dismissDialog) { Text(strings.cancel) } }
    )
}
