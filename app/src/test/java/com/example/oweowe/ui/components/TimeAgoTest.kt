package com.example.oweowe.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeAgoTest {

    private val day = 86_400_000L

    @Test
    fun nowOrFuture_isZeroDays() {
        assertEquals(0, daysSince(nowMillis = 1_000_000, thenMillis = 1_000_000))
        assertEquals(0, daysSince(nowMillis = 1_000_000, thenMillis = 2_000_000))
    }

    @Test
    fun underOneDay_isZeroDays() {
        assertEquals(0, daysSince(nowMillis = 1_000_000 + day - 1, thenMillis = 1_000_000))
    }

    @Test
    fun exactlyOneDay_isOneDay() {
        assertEquals(1, daysSince(nowMillis = 1_000_000 + day, thenMillis = 1_000_000))
    }

    @Test
    fun fiveDays_isFiveDays() {
        assertEquals(5, daysSince(nowMillis = 1_000_000 + 5 * day, thenMillis = 1_000_000))
    }
}
