package com.example.oweowe.ui.profile

import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.repository.fake.FakeAuthRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend() =
        FakeBackend(startSignedInAs = SeedData.TIM).apply { delayMs = 0 }

    private fun viewModel(db: FakeBackend) = ProfileViewModel(
        authRepository = FakeAuthRepository(db),
        userRepository = FakeUserRepository(db),
        connectivity = AlwaysOnlineObserver,
    )

    @Test
    fun editState_prefillsNameEmailAndPhone() = runTest {
        val db = backend()
        val viewModel = viewModel(db)

        val state = viewModel.editState.first { it.loadedFromMe }

        assertEquals("Tim Test", state.fullName)
        assertEquals("tim@test.dk", state.email)
        assertEquals("+45 11 11 11 11", state.phone)
    }

    @Test
    fun saveProfile_updatesTheNameAndKeepsThePhone() = runTest {
        val db = backend()
        val viewModel = viewModel(db)
        viewModel.editState.first { it.loadedFromMe }

        viewModel.updateFullName("Tim New")
        viewModel.saveProfile()
        advanceUntilIdle()

        val me = db.users.value.first { it.uid == SeedData.TIM }
        assertEquals("Tim New", me.fullName)
        assertEquals("+45 11 11 11 11", me.phone)
    }

    @Test
    fun saveProfile_blankName_flagsTheFieldAndDoesNotWrite() = runTest {
        val db = backend()
        val viewModel = viewModel(db)
        viewModel.editState.first { it.loadedFromMe }

        viewModel.updateFullName("   ")
        viewModel.saveProfile()
        advanceUntilIdle()

        assertTrue(viewModel.editState.value.nameInvalid)
        assertEquals("Tim Test", db.users.value.first { it.uid == SeedData.TIM }.fullName)
    }
}
