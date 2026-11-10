package com.kturker.multiplatform.feature.account.presentation.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.account
import com.kturker.multiplatform.feature.account.presentation.ui.EmailPasswordFields

/** Email sign-up: a name, the email and password. The way back to sign-in sits below the scroll (LoginScreen). */
@Composable
internal fun CreateAccountForm(
    state: LoginUiState,
    actions: LoginActions
) {
    val strings = LocalStringResources.current.account
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.displayName,
            onValueChange = actions::onDisplayNameChange,
            label = { Text(strings.displayName) },
            singleLine = true,
            enabled = state.signInEnabled,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )
        EmailPasswordFields(
            email = state.email,
            password = state.password,
            enabled = state.signInEnabled,
            onEmailChange = actions::onEmailChange,
            onPasswordChange = actions::onPasswordChange
        )
        Button(onClick = actions::signUp, enabled = state.canCreateAccount, modifier = Modifier.fillMaxWidth()) {
            Text(strings.createAccount)
        }
    }
}
