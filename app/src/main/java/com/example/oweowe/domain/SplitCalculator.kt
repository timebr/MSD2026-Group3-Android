package com.example.oweowe.domain

object SplitCalculator {

    /**
     * Splits [amountOre] equally among [splitAmong].
     * The rest øre go one each to the first people when sorted by UID,
     * so the shares always add up to exactly [amountOre] and every phone gets the same result.
     *
     * Example: 10000 among [u1, u2, u3] -> u1=3334, u2=3333, u3=3333
     */
    fun split(amountOre: Long, splitAmong: List<String>): Map<String, Long> {
        require(amountOre >= 0) { "amountOre must not be negative" }
        val people = splitAmong.distinct().sorted()
        require(people.isNotEmpty()) { "an expense must be split among at least one person" }

        val base = amountOre / people.size   // whole øre everyone gets
        val rest = amountOre % people.size   // 0 .. people.size - 1 øre left over

        return people.mapIndexed { index, uid ->
            uid to if (index < rest) base + 1 else base
        }.toMap()
    }
}