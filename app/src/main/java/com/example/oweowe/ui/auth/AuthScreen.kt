package com.example.oweowe.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

private enum class AuthMode { SIGN_IN, REGISTER }

/** S1 Sign in / S2 Register (FR1), switched by a toggle on the same screen. */
@Composable
fun AuthScreen(container: AppContainer) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val signInState by viewModel.signInState.collectAsStateWithLifecycle()
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }

    AuthContent(
        mode = mode,
        onModeChange = { mode = it },
        signInState = signInState,
        registerState = registerState,
        onSignInEmailChange = viewModel::updateSignInEmail,
        onSignInPasswordChange = viewModel::updateSignInPassword,
        onSignIn = viewModel::signIn,
        onRegisterFullNameChange = viewModel::updateRegisterFullName,
        onRegisterEmailChange = viewModel::updateRegisterEmail,
        onRegisterPasswordChange = viewModel::updateRegisterPassword,
        onRegisterPhoneChange = viewModel::updateRegisterPhone,
        onRegister = viewModel::register,
    )
}

@Composable
private fun AuthContent(
    mode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    signInState: SignInUiState,
    registerState: RegisterUiState,
    onSignInEmailChange: (String) -> Unit,
    onSignInPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
    onRegisterFullNameChange: (String) -> Unit,
    onRegisterEmailChange: (String) -> Unit,
    onRegisterPasswordChange: (String) -> Unit,
    onRegisterPhoneChange: (String) -> Unit,
    onRegister: () -> Unit,
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
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            FilterChip(
                selected = mode == AuthMode.SIGN_IN,
                onClick = { onModeChange(AuthMode.SIGN_IN) },
                label = { Text(stringResource(R.string.sign_in_title)) },
            )
            FilterChip(
                selected = mode == AuthMode.REGISTER,
                onClick = { onModeChange(AuthMode.REGISTER) },
                label = { Text(stringResource(R.string.register_title)) },
            )
        }

        if (mode == AuthMode.SIGN_IN) {
            SignInForm(
                state = signInState,
                onEmailChange = onSignInEmailChange,
                onPasswordChange = onSignInPasswordChange,
                onSignIn = onSignIn,
            )
        } else {
            RegisterForm(
                state = registerState,
                onFullNameChange = onRegisterFullNameChange,
                onEmailChange = onRegisterEmailChange,
                onPasswordChange = onRegisterPasswordChange,
                onPhoneChange = onRegisterPhoneChange,
                onRegister = onRegister,
            )
        }
    }
}

@Composable
private fun SignInForm(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignIn: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
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
                text = errorReasonMessage(state.error),
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
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.sign_in_demo_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RegisterForm(
    state: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRegister: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.fullName,
            onValueChange = onFullNameChange,
            label = { Text(stringResource(R.string.register_full_name_label)) },
            isError = state.nameInvalid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(stringResource(R.string.register_email_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.register_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = state.phone,
            onValueChange = onPhoneChange,
            label = { Text(stringResource(R.string.register_phone_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        if (state.error != null) {
            Text(
                text = errorReasonMessage(state.error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
        }
        Button(
            onClick = onRegister,
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
                Text(stringResource(R.string.register_button))
            }
        }
    }
}

@Preview(showBackground = true, name = "Auth sign in")
@Composable
private fun AuthSignInPreview() {
    MaterialTheme {
        AuthContent(
            mode = AuthMode.SIGN_IN,
            onModeChange = {},
            signInState = SignInUiState(email = "tim@test.dk", password = "password"),
            registerState = RegisterUiState(),
            onSignInEmailChange = {},
            onSignInPasswordChange = {},
            onSignIn = {},
            onRegisterFullNameChange = {},
            onRegisterEmailChange = {},
            onRegisterPasswordChange = {},
            onRegisterPhoneChange = {},
            onRegister = {},
        )
    }
}

@Preview(showBackground = true, name = "Auth register")
@Composable
private fun AuthRegisterPreview() {
    MaterialTheme {
        AuthContent(
            mode = AuthMode.REGISTER,
            onModeChange = {},
            signInState = SignInUiState(),
            registerState = RegisterUiState(fullName = "New User", email = "new@test.dk"),
            onSignInEmailChange = {},
            onSignInPasswordChange = {},
            onSignIn = {},
            onRegisterFullNameChange = {},
            onRegisterEmailChange = {},
            onRegisterPasswordChange = {},
            onRegisterPhoneChange = {},
            onRegister = {},
        )
    }
}
