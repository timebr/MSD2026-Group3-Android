package com.example.oweowe.ui.components

/** Whole days between two epoch-millis instants, never negative. Pure and unit-tested. */
fun daysSince(nowMillis: Long, thenMillis: Long): Int {
    val diff = nowMillis - thenMillis
    if (diff <= 0L) return 0
    return (diff / 86_400_000L).toInt()
}
