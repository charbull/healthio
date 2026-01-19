package com.healthio.ui.stats

import java.time.LocalDate

object StatsUtils {
    fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                // Determine the Mon-Sun of the current week
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                val endOfWeek = startOfWeek.plusDays(6)
                val isThisWeek = !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                Pair(date.dayOfWeek.value, isThisWeek)
            }
            TimeRange.Month -> {
                Pair(date.dayOfMonth, date.year == today.year && date.monthValue == today.monthValue)
            }
            TimeRange.Year -> {
                Pair(date.monthValue, date.year == today.year)
            }
        }
    }
}
