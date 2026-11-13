package com.example.oweowe.ui.auth

import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.fake.FakeAuthRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend() =
        FakeBackend(startSignedInAs = null).apply { delayMs = 0 }

    @Test
    fun signIn_wrongPassword_showsErrorAndSignsNobodyIn() = runTest {
        val db = backend()
        val viewModel = AuthViewModel(FakeAuthRepository(db))

        viewModel.updateSignInEmail("tim@test.dk")
        viewModel.updateSignInPassword("wrong")
        viewModel.signIn()

        val state = viewModel.signInState.value
        assertFalse(state.submitting)
        assertEquals(ErrorReason.WRONG_CREDENTIALS, state.error)
        assertNull(db.currentUserId.value)
    }

    @Test
    fun signIn_success_flipsTheSignedInUser() = runTest {
        val db = backend()
        val viewModel = AuthViewModel(FakeAuthRepository(db))

        viewModel.updateSignInEmail("tim@test.dk")
        viewModel.updateSignInPassword("password")
        viewModel.signIn()

        assertNull(viewModel.signInState.value.error)
        assertEquals(SeedData.TIM, db.currentUserId.value)
    }

    @Test
    fun register_blankName_marksTheFieldInsteadOfSubmitting() = runTest {
        val db = backend()
        val viewModel = AuthViewModel(FakeAuthRepository(db))
        val usersBefore = db.users.value.size

        viewModel.updateRegisterEmail("new@test.dk")
        viewModel.updateRegisterPassword("hunter2")
        viewModel.register()

        assertEquals(true, viewModel.registerState.value.nameInvalid)
        assertEquals(usersBefore, db.users.value.size)
    }

    @Test
    fun register_success_createsTheUserAndSignsThemIn() = runTest {
        val db = backend()
        val viewModel = AuthViewModel(FakeAuthRepository(db))

        viewModel.updateRegisterFullName("New User")
        viewModel.updateRegisterEmail("new@test.dk")
        viewModel.updateRegisterPassword("hunter2")
        viewModel.updateRegisterPhone("+45 12 34 56 78")
        viewModel.register()

        assertNull(viewModel.registerState.value.error)
        val user = db.users.value.single { it.email == "new@test.dk" }
        assertEquals("New User", user.fullName)
        assertEquals(user.uid, db.currentUserId.value)
    }

    @Test
    fun register_networkFailure_showsErrorAndSignsNobodyIn() = runTest {
        val db = backend()
        db.failNextWith = ErrorReason.NETWORK
        val viewModel = AuthViewModel(FakeAuthRepository(db))

        viewModel.updateRegisterFullName("New User")
        viewModel.updateRegisterEmail("new@test.dk")
        viewModel.updateRegisterPassword("hunter2")
        viewModel.register()

        assertEquals(ErrorReason.NETWORK, viewModel.registerState.value.error)
        assertFalse(viewModel.registerState.value.submitting)
        assertNull(db.currentUserId.value)
    }
}
