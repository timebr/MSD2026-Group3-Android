package com.example.oweowe.domain

import kotlin.math.abs

/** 1667 -> "16,67 kr.", -27000 -> "-270,00 kr." (Danish style, no Double involved). */
fun formatKr(ore: Long): String {
    val sign = if (ore < 0) "-" else ""
    val absOre = abs(ore)
    return "%s%d,%02d kr.".format(sign, absOre / 100, absOre % 100)
}
