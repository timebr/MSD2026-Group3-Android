package com.example.oweowe.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyConversionTest {
    @Test
    fun formatsOre() {
        assertEquals("16,67 kr.", formatKr(1_667))
        assertEquals("-270,00 kr.", formatKr(-27_000))
        assertEquals("0,05 kr.", formatKr(5))
        assertEquals("0,00 kr.", formatKr(0))
    }
}