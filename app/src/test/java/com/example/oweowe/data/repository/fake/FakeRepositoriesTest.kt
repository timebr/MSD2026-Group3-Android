package com.example.oweowe.data.repository.fake

import com.example.oweowe.data.model.SyncStatus
import com.example.oweowe.data.repository.AppResult
import com.example.oweowe.data.repository.ErrorReason
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun addMemberByPhone_findsTheAccountAndAddsIt() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val groupId = (groups.createGroup("Phone test", "") as AppResult.Success).value

        val result = groups.addMemberByPhone(groupId, "+45 22 22 22 22")   // Anna
        assertEquals(AppResult.Success(SeedData.users.first { it.uid == SeedData.ANNA }), result)
        assertEquals(
            listOf(SeedData.TIM, SeedData.ANNA),
            groups.observeGroup(groupId).first()?.memberIds,
        )
    }

    @Test
    fun addMemberByPhone_ignoresCountryCodeAndSpacing() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val groupId = (groups.createGroup("Phone test", "") as AppResult.Success).value

        val result = groups.addMemberByPhone(groupId, "22222222")   // Anna without +45
        assertEquals(
            AppResult.Success(SeedData.users.first { it.uid == SeedData.ANNA }),
            result,
        )
    }

    @Test
    fun addMemberWithUnknownPhone_returnsNoAccount() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val result = groups.addMemberByPhone(SeedData.FLAT, "+45 99 99 99 99")
        assertEquals(AppResult.Error(ErrorReason.NO_ACCOUNT_WITH_PHONE), result)
    }

    @Test
    fun addMemberByPhoneWhoIsAlreadyInTheGroup_returnsAlreadyMember() = runTest {
        val groups = FakeGroupRepository(backend(backgroundScope))
        val result = groups.addMemberByPhone(SeedData.FLAT, "+45 22 22 22 22")   // Anna is a member
        assertEquals(AppResult.Error(ErrorReason.ALREADY_MEMBER), result)
    }

    @Test
    fun deleteByAnotherMember_isAllowedAndRemovesTheExpense() = runTest {
        val db = backend(backgroundScope)
        val expenses = FakeExpenseRepository(db)

        // Tim deletes "Pizza night", which Anna created.
        val result = expenses.deleteExpense(SeedData.FLAT, "e2")

        assertEquals(AppResult.Success(Unit), result)
        assertTrue(db.expenses.value.none { it.id == "e2" })
    }

    @Test
    fun deleteOfASettlement_isRejected() = runTest {
        val expenses = FakeExpenseRepository(backend(backgroundScope))
        val result = expenses.deleteExpense(SeedData.FLAT, "s1")   // a settlement
        assertEquals(AppResult.Error(ErrorReason.NOT_ALLOWED), result)
    }

    @Test
    fun editByAnotherMember_isAllowedAndTheEditIsPendingThenSynced() = runTest {
        val db = backend(backgroundScope)
        val expenses = FakeExpenseRepository(db)

        // Tim edits "Pizza night", which Anna created.
        val result = expenses.updateExpense(
            groupId = SeedData.FLAT,
            expenseId = "e2",
            description = "Pizza and drinks",
            category = "Food",
            amountOre = 50_000,
            paidBy = SeedData.TIM,
            splitAmong = listOf(SeedData.TIM, SeedData.ANNA),
        )

        assertEquals(AppResult.Success(Unit), result)
        val edited = db.expenses.value.first { it.id == "e2" }
        assertEquals("Pizza and drinks", edited.description)
        assertEquals(50_000L, edited.amountOre)
        assertEquals(SeedData.TIM, edited.paidBy)
        assertEquals(listOf(SeedData.TIM, SeedData.ANNA), edited.splitAmong)
        assertEquals(SeedData.ANNA, edited.createdBy)              // the creator stays
        assertEquals(SyncStatus.PENDING, edited.syncStatus)        // NFR4

        advanceTimeBy(1_001)
        assertEquals(SyncStatus.SYNCED, db.expenses.value.first { it.id == "e2" }.syncStatus)
    }

    @Test
    fun editOfASettlement_isRejected() = runTest {
        val expenses = FakeExpenseRepository(backend(backgroundScope))
        val result = expenses.updateExpense(
            groupId = SeedData.FLAT,
            expenseId = "s1",                                      // a settlement
            description = "Changed",
            category = "",
            amountOre = 1_000,
            paidBy = SeedData.TIM,
            splitAmong = listOf(SeedData.ANNA),
        )
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

    @Test
    fun addExpense_keepsTheReceiptPath() = runTest {
        val db = backend(backgroundScope)
        val expenses = FakeExpenseRepository(db)
        expenses.addExpense(
            SeedData.FLAT, "Coffee", "Food", 5_000, SeedData.TIM, listOf(SeedData.TIM),
            receiptUri = "/data/user/0/files/receipts/1.jpg",
        )

        val saved = db.expenses.value.first { it.description == "Coffee" }
        assertEquals("/data/user/0/files/receipts/1.jpg", saved.receiptUri)
    }
}