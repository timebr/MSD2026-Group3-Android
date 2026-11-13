package com.example.oweowe.ui.home

import com.example.oweowe.R
import com.example.oweowe.data.connectivity.AlwaysOnlineObserver
import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.fake.FakeAuthRepository
import com.example.oweowe.data.repository.fake.FakeBackend
import com.example.oweowe.data.repository.fake.FakeExpenseRepository
import com.example.oweowe.data.repository.fake.FakeGroupRepository
import com.example.oweowe.data.repository.fake.FakeReminderRepository
import com.example.oweowe.data.repository.fake.FakeUserRepository
import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.domain.BalanceCalculator
import com.example.oweowe.ui.MainDispatcherRule
import com.example.oweowe.ui.components.UiMessage
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    private fun viewModel(db: FakeBackend) = HomeViewModel(
        authRepository = FakeAuthRepository(db),
        userRepository = FakeUserRepository(db),
        groupRepository = FakeGroupRepository(db),
        expenseRepository = FakeExpenseRepository(db),
        reminderRepository = FakeReminderRepository(db),
        connectivity = AlwaysOnlineObserver,
    )

    @Test
    fun uiState_showsCurrentGroupWithBalancesFromTheDomainCalculator() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)

        val state = viewModel.uiState.first { !it.loading }

        assertEquals(SeedData.TIM, state.me?.uid)
        assertEquals(SeedData.FLAT, state.group?.id)
        assertEquals(3, state.memberBalances.size)

        // Home must agree with the domain calculator, never its own math (FR5, FR6).
        val seedExpenses = db.expenses.value.filter { it.groupId == SeedData.FLAT }
        val expected = BalanceCalculator.balances(seedExpenses)
        assertEquals(expected[SeedData.TIM], state.myBalanceOre)
        expected.forEach { (uid, balance) ->
            assertEquals(balance, state.memberBalances.first { it.user.uid == uid }.balanceOre)
        }
    }

    @Test
    fun sendReminder_postsTheReminderAndEmitsAMessage() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.uiState.first { !it.loading }   // the group must be loaded before reminding

        val anna = db.users.value.first { it.uid == SeedData.ANNA }
        val messages = mutableListOf<UiMessage>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.toList(messages)
        }

        viewModel.sendReminder(
            MemberBalance(user = anna, balanceOre = -1_667, owesMeOre = 2_000),
        )
        advanceUntilIdle()
        collector.cancel()

        assertEquals(1, db.reminders.value.size)  // the write reached the repository
        assertEquals(1, messages.size)
        assertEquals(UiMessage(R.string.home_reminder_sent, listOf("Anna Test")), messages.single())
    }

    @Test
    fun ownExpenseThatSyncsInBackground_notifiesTheUser() = runTest {
        val db = backend(backgroundScope)
        val viewModel = viewModel(db)
        viewModel.uiState.first { !it.loading }

        val messages = mutableListOf<UiMessage>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.messages.toList(messages)
        }

        val result = FakeExpenseRepository(db).addExpense(
            groupId = SeedData.FLAT, description = "Dinner", category = "Food",
            amountOre = 3_000, paidBy = SeedData.TIM,
            splitAmong = listOf(SeedData.TIM, SeedData.ANNA, SeedData.JONAS),
        )
        assertEquals(AppResult.Success(Unit), result)

        val expense = db.expenses.value.last()
        assertEquals(SyncStatus.PENDING, expense.syncStatus)   // shows pending first (NFR4)
        advanceUntilIdle()                                     // the background sync completes (NFR1)
        assertEquals(SyncStatus.SYNCED, db.expenses.value.last().syncStatus)
        collector.cancel()

        assertEquals(                                          // ... and the user hears about it (NFR5)
            UiMessage(R.string.home_synced_in_background, listOf("Dinner")),
            messages.singleOrNull(),
        )
    }
}
