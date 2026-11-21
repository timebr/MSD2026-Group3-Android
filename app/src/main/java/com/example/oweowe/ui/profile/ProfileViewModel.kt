package com.example.oweowe.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.oweowe.AppContainer
import com.example.oweowe.R
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.AuthRepository
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.ui.components.UiMessage
import com.example.oweowe.ui.components.WriteState
import com.example.oweowe.ui.components.errorReasonRes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** S6 Profile. */
data class ProfileUiState(
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val offline: Boolean = false,
    val me: User? = null,
    val pushBusy: Boolean = false,
)

/** Edit-profile form (S6 / Edit profile screen). */
data class EditProfileUiState(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    /** FR2: the photo already saved with the profile, or null for initials. */
    val photoUri: String? = null,
    val nameInvalid: Boolean = false,
    val emailInvalid: Boolean = false,
    val writeState: WriteState = WriteState.Idle,
    val loadedFromMe: Boolean = false,
)

/**
 * S6 Profile (FR2): own profile, push notification preference and sign out.
 * Sign out flips the auth repository's `currentUserId` and OweOweApp switches
 * back to the Sign in screen — no direct navigation from here.
 */
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private val pushBusy = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.observeMe(),
        connectivity.isOnline,
        pushBusy,
    ) { me, online, busy ->
        ProfileUiState(loading = false, offline = !online, me = me, pushBusy = busy)
    }.catch { e ->
        if (e is CancellationException) throw e
        emit(ProfileUiState(loading = false, loadError = true))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    private val _editState = MutableStateFlow(EditProfileUiState())
    val editState: StateFlow<EditProfileUiState> = _editState.asStateFlow()

    init {
        // Pre-fill the edit form from the profile once it arrives.
        viewModelScope.launch {
            try {
                val me = userRepository.observeMe().filterNotNull().first()
                _editState.update {
                    if (it.loadedFromMe) {
                        it
                    } else {
                        it.copy(
                            fullName = me.fullName,
                            email = me.email,
                            phone = me.phone,
                            photoUri = me.photoUri,
                            loadedFromMe = true,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _messages.tryEmit(UiMessage(R.string.common_error_generic))
            }
        }
    }

    // ---- Edit profile ----

    fun updateFullName(name: String) =
        _editState.update { it.copy(fullName = name, nameInvalid = false, writeState = WriteState.Idle) }

    fun updateEmail(email: String) =
        _editState.update { it.copy(email = email, emailInvalid = false, writeState = WriteState.Idle) }

    /**
     * FR2: saves the profile. [photoPath] is the already-copied photo (the
     * screen copies the picked Uri before calling this); null keeps the
     * photo that is already saved.
     */
    fun saveProfile(photoPath: String? = null) {
        val state = _editState.value
        if (state.writeState == WriteState.Busy) return
        // Same rule the fake backend applies, so an obviously wrong address
        // is caught before the write instead of coming back as an error.
        val emailInvalid = !state.email.trim().contains("@")
        if (state.fullName.isBlank() || emailInvalid) {
            _editState.update { it.copy(nameInvalid = state.fullName.isBlank(), emailInvalid = emailInvalid) }
            return
        }
        _editState.update { it.copy(writeState = WriteState.Busy) }
        viewModelScope.launch {
            val result = userRepository.updateProfile(
                fullName = state.fullName.trim(),
                email = state.email.trim(),
                photoUri = photoPath,
            )
            when (result) {
                is AppResult.Success -> {
                    _editState.update { it.copy(writeState = WriteState.Done) }
                    _messages.emit(UiMessage(R.string.edit_profile_saved))
                }
                is AppResult.Error ->
                    _editState.update { it.copy(writeState = WriteState.Failed(result.reason)) }
            }
        }
    }

    /**
     * FR2: the picked photo could not be copied into app storage. Saving the
     * name without the photo would silently drop the user's choice, so the
     * save fails instead and the form offers Retry.
     */
    fun onPhotoSaveFailed() =
        _editState.update { it.copy(writeState = WriteState.Failed(ErrorReason.UNKNOWN)) }

    fun resetWriteState() = _editState.update { it.copy(writeState = WriteState.Idle) }

    // ---- Notification settings ----

    fun setPushEnabled(enabled: Boolean) {
        if (pushBusy.value) return
        pushBusy.value = true
        viewModelScope.launch {
            when (val result = userRepository.setPushEnabled(enabled)) {
                is AppResult.Success -> Unit                     // the profile flow shows the new value
                is AppResult.Error -> {
                    _messages.emit(UiMessage(errorReasonRes(result.reason)))
                }
            }
            pushBusy.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ProfileViewModel(
                        authRepository = container.authRepository,
                        userRepository = container.userRepository,
                        connectivity = container.connectivityObserver,
                    )
                }
            }
    }
}
