package com.example.oweowe.domain

import com.example.oweowe.data.repository.fake.SeedData
import com.example.oweowe.data.repository.fake.SeedData.ANNA
import com.example.oweowe.data.repository.fake.SeedData.JONAS
import com.example.oweowe.data.repository.fake.SeedData.TIM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** All expected numbers come from the table in the stage 2 guide. */
class BalanceCalculatorTest {

    private val expenses = SeedData.expenses

    @Test
    fun pairwiseDebtsOfSeedScenario() {
        val debts = BalanceCalculator.pairwiseDebts(expenses).toSet()
        assertEquals(
            setOf(
                Debt(from = TIM, to = ANNA, amountOre = 1_667),
                Debt(from = JONAS, to = ANNA, amountOre = 27_000),
            ),
            debts,
        )
    }

    @Test
    fun settlementCancelsTheDebt() {
        assertEquals(0L, BalanceCalculator.amountOwed(expenses, from = JONAS, to = TIM))
        assertEquals(0L, BalanceCalculator.amountOwed(expenses, from = TIM, to = JONAS))
    }

    @Test
    fun balancesPerPerson() {
        val balances = BalanceCalculator.balances(expenses)
        assertEquals(-1_667L, balances[TIM])
        assertEquals(28_667L, balances[ANNA])
        assertEquals(-27_000L, balances[JONAS])
    }

    @Test
    fun balancesSumToZero() {
        assertEquals(0L, BalanceCalculator.balances(expenses).values.sum())
    }

    @Test
    fun totalSpentIgnoresSettlements() {
        assertEquals(129_000L, BalanceCalculator.totalSpent(expenses))
    }

    @Test
    fun isSettled() {
        assertFalse(BalanceCalculator.isSettled(expenses, TIM))   // Tim still owes Anna
        assertTrue(BalanceCalculator.isSettled(emptyList(), TIM))
    }
}