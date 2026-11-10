package com.kturker.multiplatform.feature.account.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kturker.multiplatform.core.designsystem.language.LocalStringResources
import com.kturker.multiplatform.core.multilang.resource.account

/** Email and password, Google and Apple: the sign-in screen's and the re-sign-in before account deletion. */
@Composable
internal fun SignInMethods(
    email: String,
    password: String,
    enabled: Boolean,
    callbacks: SignInCallbacks
) {
    val strings = LocalStringResources.current.account
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        EmailPasswordFields(
            email = email,
            password = password,
            enabled = enabled,
            onEmailChange = callbacks.onEmailChange,
            onPasswordChange = callbacks.onPasswordChange
        )
        Button(onClick = callbacks.onSignIn, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(strings.signIn)
        }
        callbacks.onCreateAccount?.let { onCreateAccount ->
            OutlinedButton(onClick = onCreateAccount, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(strings.createAccount)
            }
        }
        HorizontalDivider()
        // Google's branding: the "Continue with Google" wording on a neutral (outlined) button
        OutlinedButton(onClick = callbacks.onGoogle, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(strings.continueWithGoogle)
        }
        // Apple's guidelines: a black button with white "Sign in with Apple"; shown on Android too (web flow)
        Button(
            onClick = callbacks.onApple,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.signInWithApple)
        }
    }
}

/** The email and password fields, shared by the sign-in methods and the create-account form. */
@Composable
internal fun EmailPasswordFields(
    email: String,
    password: String,
    enabled: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit
) {
    val strings = LocalStringResources.current.account
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(strings.email) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(strings.password) },
        singleLine = true,
        enabled = enabled,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
}
