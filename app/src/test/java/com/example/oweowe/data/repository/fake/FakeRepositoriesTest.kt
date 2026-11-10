package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import org.junit.Test
import org.junit.Assert.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope


class FakeRepositoriesTest {

    /** Signed in as Tim, no artificial delay; background work runs on the test's virtual clock. */
    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply {
            delayMs = 0
            syncDelayMs = 1_000
        }

    @Test
    fun addMemberWithUnknownEmail_returnsNoAccount() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val result = groups.addMemberByEmail(SeedData.FLAT, "nobody@test.dk")
        assertEquals(AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_EMAIL), result)
    }

    @Test
    fun deleteByNonCreator_isRejected() = runTest {
        val expenses = FakeExpenseRepository(backend(backgroundScope))
        val result = expenses.deleteExpense(SeedData.FLAT, "e2")   // e2 was created by Anna
        assertEquals(AppResult.Error(ErrorReason.NOT_ALLOWED), result)
    }

    @Test
    fun newExpense_isPendingThenSynced() = runTest {
        val expenses = FakeExpenseRepository(backend(backgroundScope))
        expenses.addExpense(SeedData.FLAT, "Coffee", "Food", 5_000, SeedData.TIM, listOf(SeedData.TIM, SeedData.ANNA))

        val before = expenses.observeExpenses(SeedData.FLAT).first().first { it.description == "Coffee" }
        assertEquals(SyncStatus.PENDING, before.syncStatus)

        advanceTimeBy(1_001)   // virtual time: the test does not really wait
        val after = expenses.observeExpenses(SeedData.FLAT).first().first { it.description == "Coffee" }
        assertEquals(SyncStatus.SYNCED, after.syncStatus)
    }
}