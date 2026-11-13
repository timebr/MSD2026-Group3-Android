package com.example.oweowe.ui.groups

import com.example.oweowe.data.connectivity.AlwaysOfflineObserver
import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.ui.MainDispatcherRule
import com.example.oweowe.ui.components.WriteState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    @Test
    fun uiState_listsMyGroupsFromTheRepository() = runTest {
        val db = backend(backgroundScope)
        val viewModel = GroupsViewModel(FakeGroupRepository(db), AlwaysOnlineObserver)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(false, state.offline)
        assertEquals(listOf(SeedData.FLAT), state.groups.map { it.id })
    }

    @Test
    fun uiState_flagsOfflineButStillShowsCachedGroups() = runTest {
        val db = backend(backgroundScope)
        val viewModel = GroupsViewModel(FakeGroupRepository(db), AlwaysOfflineObserver)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(true, state.offline)                       // banner shows, but data stays (NFR3)
        assertEquals(listOf(SeedData.FLAT), state.groups.map { it.id })
    }

    @Test
    fun createGroup_blankName_marksTheFieldInsteadOfSubmitting() = runTest {
        val db = backend(backgroundScope)
        val viewModel = GroupsViewModel(FakeGroupRepository(db), AlwaysOnlineObserver)

        viewModel.updateDescription("No name given")
        viewModel.createGroup()

        assertEquals(true, viewModel.createState.value.nameInvalid)
        assertEquals(1, db.groups.value.size)                   // only the seed group
    }

    @Test
    fun createGroup_success_emitsTheNewGroupIdAndCreatesTheGroup() = runTest {
        val db = backend(backgroundScope)
        val viewModel = GroupsViewModel(FakeGroupRepository(db), AlwaysOnlineObserver)
        val createdIds = mutableListOf<String>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.createdGroupId.toList(createdIds)
        }

        viewModel.updateName("Holiday")
        viewModel.updateDescription("Summer trip")
        viewModel.createGroup()
        advanceUntilIdle()
        collector.cancel()

        assertEquals(1, createdIds.size)
        val group = db.groups.value.single { it.name == "Holiday" }
        assertEquals(createdIds.single(), group.id)
        assertEquals("Summer trip", group.description)
        assertEquals(listOf(SeedData.TIM), group.memberIds)     // the creator starts alone
    }

    @Test
    fun createGroup_networkFailure_showsFailedStateForRetry() = runTest {
        val db = backend(backgroundScope)
        db.failNextWith = ErrorReason.NETWORK
        val viewModel = GroupsViewModel(FakeGroupRepository(db), AlwaysOnlineObserver)

        viewModel.updateName("Holiday")
        viewModel.createGroup()

        assertEquals(WriteState.Failed(ErrorReason.NETWORK), viewModel.createState.value.writeState)
        assertEquals(1, db.groups.value.size)
    }
}
