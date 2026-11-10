package com.example.oweowe.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitCalculatorTest {

    @Test
    fun evenSplit() {
        val shares = SplitCalculator.split(30_000, listOf("u1", "u2", "u3"))
        assertEquals(mapOf("u1" to 10_000L, "u2" to 10_000L, "u3" to 10_000L), shares)
    }

    @Test
    fun restOreGoesToFirstUid() {
        val shares = SplitCalculator.split(10_000, listOf("u1", "u2", "u3"))
        assertEquals(mapOf("u1" to 3_334L, "u2" to 3_333L, "u3" to 3_333L), shares)
    }

    @Test
    fun sharesAlwaysAddUpToTheAmount() {
        for (amount in listOf(1L, 2L, 99L, 100L, 10_001L, 12_345L)) {
            for (people in 1..7) {
                val uids = (1..people).map { "u$it" }
                assertEquals(amount, SplitCalculator.split(amount, uids).values.sum())
            }
        }
    }

    @Test
    fun orderOfInputDoesNotMatter() {
        assertEquals(
            SplitCalculator.split(10_000, listOf("u1", "u2", "u3")),
            SplitCalculator.split(10_000, listOf("u3", "u1", "u2")),
        )
    }

    @Test
    fun duplicatesAreIgnored() {
        assertEquals(mapOf("u1" to 5_000L, "u2" to 5_000L), SplitCalculator.split(10_000, listOf("u1", "u2", "u2")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySplitIsRejected() {
        SplitCalculator.split(10_000, emptyList())
    }
}