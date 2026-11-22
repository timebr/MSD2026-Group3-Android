package com.example.oweowe.ui.groups

import com.example.oweowe.SelectedGroupStore
import com.example.oweowe.data.connectivity.AlwaysOfflineObserver
import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.connectivity.ConnectivityObserver
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.MainDispatcherRule
import com.example.oweowe.ui.components.WriteState
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    private fun viewModel(
        db: FakeBackend,
        observer: ConnectivityObserver,
        selectedGroupStore: SelectedGroupStore? = null,
        groupRepository: GroupRepository = FakeGroupRepository(db),
    ) = GroupsViewModel(
        groupRepository = groupRepository,
        userRepository = FakeUserRepository(db),
        expenseRepository = FakeExpenseRepository(db),
        connectivity = observer,
        selectedGroupStore = selectedGroupStore,
    )

    @Test
    fun uiState_listsMyGroupsWithMyNetBalanceFromTheDomainCalculator() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, AlwaysOnlineObserver)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(false, state.offline)
        assertEquals(listOf(SeedData.FLAT), state.groups.map { it.group.id })
        val expected = BalanceCalculator.balances(db.expenses.value.filter { it.groupId == SeedData.FLAT })[SeedData.TIM]
        assertEquals(expected, state.groups.single().myNetOre)
    }

    /**
     * NFR4: a failed read must not dead-end S4. The error state offers a retry
     * that re-collects the repository flow and brings the cached data back.
     */
    @Test
    fun readError_isRecoverableWithTheRetryAction() = runTest {
        val db = backend(backgroundScope)
        var failuresLeft = 1
        val flakyGroups = object : GroupRepository by FakeGroupRepository(db) {
            override fun observeMyGroups(): Flow<List<Group>> = flow {
                if (failuresLeft > 0) {
                    failuresLeft--
                    throw IOException("boom")
                }
                emitAll(FakeGroupRepository(db).observeMyGroups())
            }
        }
        val viewModel = viewModel(db, AlwaysOnlineObserver, groupRepository = flakyGroups)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertTrue(viewModel.uiState.first { it.loadError }.loadError)

        viewModel.retry()

        val recovered = viewModel.uiState.first { !it.loading && !it.loadError }
        assertEquals(listOf(SeedData.FLAT), recovered.groups.map { it.group.id })
    }

    @Test
    fun fallbackGroup_isMirroredToTheSelectedGroupStore() = runTest {
        val db = backend(backgroundScope)
        val store = SelectedGroupStore()

        viewModel(db, AlwaysOnlineObserver, store).uiState.first { !it.loading }

        assertEquals(SeedData.FLAT, store.selectedGroupId.value)
    }

    @Test
    fun uiState_flagsOfflineButStillShowsCachedGroups() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, AlwaysOfflineObserver)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(true, state.offline)                       // banner shows, but data stays (NFR3)
        assertEquals(listOf(SeedData.FLAT), state.groups.map { it.group.id })
    }

    @Test
    fun uiState_selectGroupMakesTheChosenGroupSelected() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, AlwaysOnlineObserver)
        val createdIds = mutableListOf<String>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.createdGroupId.toList(createdIds)
        }

        viewModel.updateName("Holiday")
        viewModel.createGroup()
        advanceUntilIdle()
        collector.cancel()
        val newId = createdIds.single()

        // Default selection is the group with the most recent activity (the new one).
        val initial = viewModel.uiState.first { it.groups.size == 2 }
        assertEquals(newId, initial.selectedGroup?.group?.id)

        // Picking a group makes it the selected one; all groups stay listed.
        viewModel.selectGroup(SeedData.FLAT)
        val state = viewModel.uiState.first { it.selectedGroup?.group?.id == SeedData.FLAT }
        assertEquals(SeedData.FLAT, state.selectedGroup?.group?.id)
        assertEquals(2, state.groups.size)
    }

    @Test
    fun createGroup_blankName_marksTheFieldInsteadOfSubmitting() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, AlwaysOnlineObserver)

        viewModel.updateDescription("No name given")
        viewModel.createGroup()

        assertEquals(true, viewModel.createState.value.nameInvalid)
        assertEquals(1, db.groups.value.size)                   // only the seed group
    }

    @Test
    fun createGroup_success_emitsTheNewGroupIdAndCreatesTheGroup() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db, AlwaysOnlineObserver)
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
        val viewModel = viewModel(db, AlwaysOnlineObserver)

        viewModel.updateName("Holiday")
        viewModel.createGroup()

        assertEquals(WriteState.Failed(ErrorReason.NETWORK), viewModel.createState.value.writeState)
        assertEquals(1, db.groups.value.size)
    }
}
