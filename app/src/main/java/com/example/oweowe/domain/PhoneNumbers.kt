package com.example.oweowe.domain

/**
 * Canonical form used to look a member up by phone: digits only, with a
 * leading Danish country code (+45 / 0045) removed.
 *
 * "+45 22 22 22 22" and "22222222" both become "22222222".
 */
fun normalizePhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return when {
        digits.length > 8 && digits.startsWith("45") -> digits.drop(2)
        digits.length > 10 && digits.startsWith("0045") -> digits.drop(4)
        else -> digits
    }
}
