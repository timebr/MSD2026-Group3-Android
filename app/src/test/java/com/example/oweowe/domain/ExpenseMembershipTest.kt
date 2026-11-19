package com.example.oweowe.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseMembershipTest {

    private val tim = "tim"
    private val anna = "anna"
    private val outsider = "outsider"
    private val members = listOf(tim, anna)

    @Test
    fun writeAllowed_whenCallerPayerAndSplitAreMembers() {
        assertTrue(canWriteExpense(tim, members, paidBy = anna, splitAmong = listOf(tim, anna)))
    }

    @Test
    fun writeRejected_whenCallerIsNotAMember() {
        assertFalse(canWriteExpense(outsider, members, paidBy = tim, splitAmong = listOf(tim)))
    }

    @Test
    fun writeRejected_whenPayerIsNotAMember() {
        assertFalse(canWriteExpense(tim, members, paidBy = outsider, splitAmong = listOf(tim)))
    }

    @Test
    fun writeRejected_whenOneSplitMemberIsNotAMember() {
        assertFalse(
            canWriteExpense(tim, members, paidBy = tim, splitAmong = listOf(tim, outsider)),
        )
    }

    @Test
    fun writeRejected_whenSplitIsEmpty() {
        assertFalse(canWriteExpense(tim, members, paidBy = tim, splitAmong = emptyList()))
    }

    @Test
    fun writeAllowed_whenPayerAlsoAppearsInTheSplit() {
        assertTrue(canWriteExpense(tim, members, paidBy = tim, splitAmong = listOf(tim, anna)))
    }

    @Test
    fun settleAllowed_whenBothUsersAreMembers() {
        assertTrue(canSettleUp(tim, members, withUserId = anna))
    }

    @Test
    fun settleRejected_whenCallerIsNotAMember() {
        assertFalse(canSettleUp(outsider, members, withUserId = anna))
    }

    @Test
    fun settleRejected_whenCounterpartyIsNotAMember() {
        assertFalse(canSettleUp(tim, members, withUserId = outsider))
    }
}
