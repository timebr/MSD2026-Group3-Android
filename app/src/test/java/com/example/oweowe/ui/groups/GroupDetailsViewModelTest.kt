package com.example.oweowe.ui.groups

import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.model.User
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.model.Group
import com.example.oweowe.data.repository.GroupRepository
import com.example.oweowe.data.repository.UserRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeReminderRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.MainDispatcherRule
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GroupDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    private fun viewModel(
        db: FakeBackend,
        groupId: String = SeedData.FLAT,
        userRepository: UserRepository = FakeUserRepository(db),
        groupRepository: GroupRepository = FakeGroupRepository(db),
    ) = GroupDetailsViewModel(
        groupId = groupId,
        userRepository = userRepository,
        groupRepository = groupRepository,
        expenseRepository = FakeExpenseRepository(db),
        reminderRepository = FakeReminderRepository(db),
        connectivity = AlwaysOnlineObserver,
    )

    @Test
    fun transactions_areNewestFirstWithThePayerName() = runTest {
        val db = backend(backgroundScope)

        val state = viewModel(db).uiState.first { !it.loading }

        assertEquals(
            listOf("s1", "e5", "e4", "e3", "e2", "e1"),
            state.transactions.map { it.expense.id },
        )
        assertEquals(
            "Anna Test",
            state.transactions.first { it.expense.id == "e2" }.payerName,
        )
    }

    @Test
    fun transactions_areOnlyTheGroupsOwn() = runTest {
        val db = backend(backgroundScope)
        val groups = FakeGroupRepository(db)
        val otherGroupId = (groups.createGroup("Ski trip", "") as AppResult.Success).value
        FakeExpenseRepository(db).addExpense(
            groupId = otherGroupId,
            description = "Ski pass",
            category = "Sport",
            amountOre = 10_000,
            paidBy = SeedData.TIM,
            splitAmong = listOf(SeedData.TIM),
        )

        val state = viewModel(db).uiState.first { !it.loading }

        assertTrue(state.transactions.none { it.expense.groupId == otherGroupId })
        assertEquals(6, state.transactions.size)
    }

    @Test
    fun deleteExpense_removesItForAnyMember() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        val state = viewModel.uiState.first { !it.loading }
        val expense = state.transactions.first { it.expense.id == "e2" }.expense   // created by Anna

        viewModel.deleteExpense(expense)
        advanceUntilIdle()

        val remaining = FakeExpenseRepository(db).observeExpenses(SeedData.FLAT).first()
        assertTrue(remaining.none { it.id == "e2" })
        assertEquals(false, viewModel.uiState.value.busy)
    }

    @Test
    fun settleUp_recordsASettlementAndClearsTheDebt() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        val state = viewModel.uiState.first { !it.loading }
        // In the seed data Tim owes Anna 16.67 kr.
        val anna = state.members.first { it.user.uid == SeedData.ANNA }
        assertEquals(1_667L, anna.iOweOre)

        viewModel.settleUp(anna)
        advanceUntilIdle()

        val expenses = FakeExpenseRepository(db).observeExpenses(SeedData.FLAT).first()
        val settlement = expenses.first { it.type == ExpenseType.SETTLEMENT && it.paidBy == SeedData.TIM }
        assertEquals(1_667L, settlement.amountOre)
        assertEquals(listOf(SeedData.ANNA), settlement.splitAmong)
        assertEquals(0L, BalanceCalculator.amountOwed(expenses, SeedData.TIM, SeedData.ANNA))
        assertEquals(false, viewModel.uiState.value.busy)
    }

    @Test
    fun switchingGroup_showsTheNewGroupsMembersAndBalances() = runTest {
        val db = backend(backgroundScope)
        val groups = FakeGroupRepository(db)
        val secondId = (groups.createGroup("Ski trip", "") as AppResult.Success).value
        groups.addMemberByEmail(secondId, "anna@test.dk")
        FakeExpenseRepository(db).addExpense(
            groupId = secondId,
            description = "Lift pass",
            category = "Travel",
            amountOre = 10_000,
            paidBy = SeedData.TIM,
            splitAmong = listOf(SeedData.TIM, SeedData.ANNA),
        )

        val state = viewModel(db, secondId).uiState.first { !it.loading }

        assertEquals(setOf(SeedData.TIM, SeedData.ANNA), state.members.map { it.user.uid }.toSet())
        assertEquals(5_000L, state.members.first { it.user.uid == SeedData.ANNA }.owesMeOre)
        assertTrue(state.members.none { it.user.uid == SeedData.JONAS })
    }

    @Test
    fun membersWithoutAProfileRemainVisible() = runTest {
        val db = backend(backgroundScope)
        db.users.update { users -> users.filterNot { it.uid == SeedData.ANNA } }

        val state = viewModel(db).uiState.first { !it.loading }

        val anna = state.members.first { it.user.uid == SeedData.ANNA }
        // The raw UID is an internal id, not a name to show on S4/S5 (FR2).
        assertEquals(User.UNKNOWN_FULL_NAME, anna.user.fullName)
        assertTrue(anna.iOweOre > 0)
    }

    /**
     * NFR: the edit form listens to the group in a second collector. A failed
     * read there used to escape a viewModelScope launch and crash the app.
     */
    @Test
    fun groupReadError_doesNotCrashTheEditFormCollector() = runTest {
        val db = backend(backgroundScope)
        val failingGroups = object : GroupRepository by FakeGroupRepository(db) {
            override fun observeGroup(groupId: String): Flow<Group?> = flow { throw IOException("boom") }
        }

        val viewModel = viewModel(db, groupRepository = failingGroups)
        viewModel.uiState.first { it.loadError }
        advanceUntilIdle()

        assertTrue(!viewModel.editState.value.loadedFromGroup)
    }

    @Test
    fun repositoryReadError_showsTheErrorState() = runTest {
        val db = backend(backgroundScope)
        val failingUsers = object : UserRepository by FakeUserRepository(db) {
            override fun observeMe(): Flow<User?> = flow { throw IOException("boom") }
        }

        val state = viewModel(db, userRepository = failingUsers).uiState.first { it.loadError }

        assertTrue(!state.loading)
        assertTrue(state.members.isEmpty())
    }
}
