package com.example.oweowe.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.example.oweowe.AppContainer
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ErrorReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S1 Sign in. */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val submitting: Boolean = false,
    val error: ErrorReason? = null,
)

/** S2 Register. */
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val submitting: Boolean = false,
    val error: ErrorReason? = null,
    val nameInvalid: Boolean = false,
)

/**
 * S1 + S2. Successful sign in / register is not navigated from here: the
 * repository's `currentUserId` flips and OweOweApp switches to Home, so the
 * same path works for a real backend later.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _signInState = MutableStateFlow(SignInUiState())
    val signInState: StateFlow<SignInUiState> = _signInState.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    fun updateSignInEmail(email: String) =
        _signInState.update { it.copy(email = email, error = null) }

    fun updateSignInPassword(password: String) =
        _signInState.update { it.copy(password = password, error = null) }

    fun signIn() {
        val state = _signInState.value
        if (state.submitting) return                       // never double-submit
        _signInState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.signIn(state.email, state.password)
            _signInState.update {
                if (result is AppResult.Error) {
                    it.copy(submitting = false, error = result.reason)
                } else {
                    it.copy(submitting = false)            // success: gate switches to Home
                }
            }
        }
    }

    fun updateRegisterFullName(name: String) =
        _registerState.update { it.copy(fullName = name, nameInvalid = false, error = null) }

    fun updateRegisterEmail(email: String) =
        _registerState.update { it.copy(email = email, error = null) }

    fun updateRegisterPassword(password: String) =
        _registerState.update { it.copy(password = password, error = null) }

    fun updateRegisterPhone(phone: String) =
        _registerState.update { it.copy(phone = phone, error = null) }

    fun register() {
        val state = _registerState.value
        if (state.submitting) return
        if (state.fullName.isBlank()) {
            _registerState.update { it.copy(nameInvalid = true) }
            return
        }
        _registerState.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.register(
                state.fullName, state.email, state.password, state.phone,
            )
            _registerState.update {
                if (result is AppResult.Error) {
                    it.copy(submitting = false, error = result.reason)
                } else {
                    it.copy(submitting = false)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory { initializer { AuthViewModel(container.authRepository) } }
    }
}
