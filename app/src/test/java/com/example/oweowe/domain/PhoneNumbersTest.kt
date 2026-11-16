package com.example.oweowe.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumbersTest {

    @Test
    fun removesDanishCountryCodeAndFormatting() {
        assertEquals("22222222", normalizePhone("+45 22 22 22 22"))
        assertEquals("22222222", normalizePhone("0045 22 22 22 22"))
        assertEquals("22222222", normalizePhone("22222222"))
    }

    @Test
    fun keepsForeignNumbersAsDigits() {
        assertEquals("4612345678", normalizePhone("+46 12 34 56 78"))
    }
}
