package com.healthio.ui.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object StatsUtils {
    fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                // Determine the Mon-Sun of the current week
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
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
