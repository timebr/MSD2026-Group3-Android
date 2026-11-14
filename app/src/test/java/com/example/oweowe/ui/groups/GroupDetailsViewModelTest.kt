package com.example.oweowe.ui.groups

import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeReminderRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
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

    private fun viewModel(db: FakeBackend, groupId: String = SeedData.FLAT) = GroupDetailsViewModel(
        groupId = groupId,
        userRepository = FakeUserRepository(db),
        groupRepository = FakeGroupRepository(db),
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
}
