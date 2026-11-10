package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.ExpenseType
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import com.example.oweowe.domain.BalanceCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeStage3Test {

    private fun backend(scope: CoroutineScope) =
        FakeBackend(startSignedInAs = SeedData.TIM, scope = scope).apply { delayMs = 0 }

    @Test
    fun removeMemberWithOpenBalance_isRejected() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val result = groups.removeMember(SeedData.FLAT, SeedData.JONAS)   // Jonas owes Anna 270 kr.
        assertEquals(AppResult.Error(ErrorReason.BALANCE_NOT_ZERO), result)
    }

    @Test
    fun settleUp_createsSettlementAndClearsTheDebt() = runTest {
        val db = backend(backgroundScope)
        val expenses = FakeExpenseRepository(db)

        val result = expenses.settleUp(SeedData.FLAT, withUserId = SeedData.ANNA)   // Tim owes Anna 16.67
        assertEquals(AppResult.Success(Unit), result)

        val settlement = db.expenses.value.last()
        assertEquals(ExpenseType.SETTLEMENT, settlement.type)
        assertEquals(1_667L, settlement.amountOre)
        assertEquals(0L, BalanceCalculator.amountOwed(db.expenses.value, from = SeedData.TIM, to = SeedData.ANNA))
    }

    @Test
    fun settleUpWhenNothingIsOwed_returnsNothingToSettle() = runTest {
        val expenses = FakeExpenseRepository(backend(backgroundScope))
        val result = expenses.settleUp(SeedData.FLAT, withUserId = SeedData.JONAS)   // Tim and Jonas are even
        assertEquals(AppResult.Error(ErrorReason.NOTHING_TO_SETTLE), result)
    }
}