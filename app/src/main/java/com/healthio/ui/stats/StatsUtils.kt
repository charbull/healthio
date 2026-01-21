package com.healthio.ui.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object StatsUtils {
    fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                // Determine the Mon-Sun of the current week relative to 'today'
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                val isThisWeek = !date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)
                Pair(date.dayOfWeek.value, isThisWeek)
            }
            TimeRange.Month -> {
                val isThisMonth = date.year == today.year && date.monthValue == today.monthValue
                Pair(date.dayOfMonth, isThisMonth)
            }
            TimeRange.Year -> {
                val isThisYear = date.year == today.year
                Pair(date.monthValue, isThisYear)
            }
        }
    }
}
