package com.example.oweowe.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.ui.components.AppTopBar
import com.example.oweowe.ui.components.errorReasonMessage

/** S2 Register (FR1). */
@Composable
fun RegisterScreen(
    container: AppContainer,
    onNavigateBack: () -> Unit,
) {
    val viewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container))
    val state by viewModel.registerState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        AppTopBar(
            title = stringResource(R.string.register_title),
            showBack = true,
            onBack = onNavigateBack,
        )
    }) { padding ->
        RegisterContent(
            state = state,
            onFullNameChange = viewModel::updateRegisterFullName,
            onEmailChange = viewModel::updateRegisterEmail,
            onPasswordChange = viewModel::updateRegisterPassword,
            onPhoneChange = viewModel::updateRegisterPhone,
            onRegister = viewModel::register,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun RegisterContent(
    state: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
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
                text = errorReasonMessage(state.error!!),
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

@Preview(showBackground = true, name = "Register")
@Composable
private fun RegisterContentPreview() {
    MaterialTheme {
        RegisterContent(
            state = RegisterUiState(
                fullName = "New User",
                email = "new@test.dk",
                phone = "+45 12 34 56 78",
            ),
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPhoneChange = {},
            onRegister = {},
        )
    }
}

@Preview(showBackground = true, name = "Register error")
@Composable
private fun RegisterErrorPreview() {
    MaterialTheme {
        RegisterContent(
            state = RegisterUiState(
                email = "tim@test.dk",
                error = ErrorReason.EMAIL_ALREADY_IN_USE,
            ),
            onFullNameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPhoneChange = {},
            onRegister = {},
        )
    }
}
