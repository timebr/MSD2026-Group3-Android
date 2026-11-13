package com.example.oweowe.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyConversionTest {
    @Test
    fun formatsOre() {
        assertEquals("16,67 kr.", formatKr(1_667))
        assertEquals("-270,00 kr.", formatKr(-27_000))
        assertEquals("0,05 kr.", formatKr(5))
        assertEquals("0,00 kr.", formatKr(0))
    }

    @Test
    fun parsesValidAmounts() {
        assertEquals(1_600L, parseKr("16"))
        assertEquals(1_650L, parseKr("16,5"))
        assertEquals(1_650L, parseKr("16.50"))
        assertEquals(1L, parseKr("0,01"))
        assertEquals(0L, parseKr("0"))
        assertEquals(270_00L, parseKr(" 270 kr "))
    }

    @Test
    fun rejectsInvalidAmounts() {
        assertNull(parseKr(""))
        assertNull(parseKr("   "))
        assertNull(parseKr("abc"))
        assertNull(parseKr("-5"))              // negative amounts are a UI toggle, not typed text
        assertNull(parseKr("16,,5"))
        assertNull(parseKr("1.2.3"))
        assertNull(parseKr("12345678901"))     // more than 9 whole digits: refuse instead of overflowing
    }
}