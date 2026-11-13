package com.example.oweowe.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.errorReasonMessage

/** S1 Sign in (FR1). */
@Composable
fun SignInScreen(
    container: AppContainer,
    onNavigateToRegister: () -> Unit,
) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val state by viewModel.signInState.collectAsStateWithLifecycle()

    SignInContent(
        state = state,
        onEmailChange = viewModel::updateSignInEmail,
        onPasswordChange = viewModel::updateSignInPassword,
        onSignIn = viewModel::signIn,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 32.dp),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.sign_in_email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.sign_in_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        if (state.error != null) {
            Text(
                text = errorReasonMessage(state.error!!),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        Button(
            onClick = onSignIn,
            enabled = !state.submitting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            if (state.submitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp).widthIn(min = 20.dp),
                )
            } else {
                Text(stringResource(R.string.sign_in_button))
            }
        }
        TextButton(onClick = onNavigateToRegister, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.sign_in_switch_to_register))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sign_in_demo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true, name = "Sign in")
@Composable
private fun SignInContentPreview() {
    MaterialTheme {
        SignInContent(
            state = SignInUiState(email = "tim@test.dk", password = "password"),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onNavigateToRegister = {},
        )
    }
}

@Preview(showBackground = true, name = "Sign in submitting")
@Composable
private fun SignInSubmittingPreview() {
    MaterialTheme {
        SignInContent(
            state = SignInUiState(email = "tim@test.dk", password = "password", submitting = true),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onNavigateToRegister = {},
        )
    }
}

@Preview(showBackground = true, name = "Sign in error")
@Composable
private fun SignInErrorPreview() {
    MaterialTheme {
        SignInContent(
            state = SignInUiState(email = "tim@test.dk", password = "wrong", error = ErrorReason.WRONG_CREDENTIALS),
            onEmailChange = {},
            onPasswordChange = {},
            onSignIn = {},
            onNavigateToRegister = {},
        )
    }
}
