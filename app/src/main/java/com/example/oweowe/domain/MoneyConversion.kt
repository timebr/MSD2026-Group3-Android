package com.example.oweowe.domain

import kotlin.math.abs

/** 1667 -> "16,67 kr.", -27000 -> "-270,00 kr." (Danish style, no Double involved). */
fun formatKr(ore: Long): String {
    val sign = if (ore < 0) "-" else ""
    val absOre = abs(ore)
    return "%s%d,%02d kr.".format(sign, absOre / 100, absOre % 100)
}

/**
 * "16" -> 1600, "16,5" -> 1650, "16.50" -> 1650, "0,01" -> 1.
 * Accepts both "," and "." as the decimal separator. Null when [text] is not a
 * valid non-negative amount, so the caller can show a field error. No Double involved.
 */
fun parseKr(text: String): Long? {
    val cleaned = text.trim()
        .replace(" ", "")
        .replace("kr", "", ignoreCase = true)
        .replace('.', ',')
    if (cleaned.isEmpty()) return null

    val parts = cleaned.split(',')
    if (parts.size > 2) return null
    val whole = parts[0].ifEmpty { return null }
    val fraction = if (parts.size == 2) parts[1].padEnd(2, '0').take(2) else "00"

    if (whole.any { !it.isDigit() } || fraction.any { !it.isDigit() }) return null
    if (whole.length > 9) return null                    // far beyond DKK amounts; blocks overflow
    return whole.toLong() * 100 + fraction.toLong()
}
