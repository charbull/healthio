package com.healthio.ui.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object StatsUtils {
    fun getBucketIndex(date: LocalDate, range: TimeRange, today: LocalDate): Pair<Int, Boolean> {
        return when (range) {
            TimeRange.Week -> {
                // Rolling 7 days: today - 6 days to today
                val startOfRollingWeek = today.minusDays(6)
                val isWithinRollingWeek = !date.isBefore(startOfRollingWeek) && !date.isAfter(today)
                if (isWithinRollingWeek) {
                    val index = ChronoUnit.DAYS.between(startOfRollingWeek, date).toInt() + 1
                    Pair(index, true)
                } else {
                    Pair(-1, false)
                }
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

    fun calculateProRatedBMR(baseDailyBurn: Int, hour: Int, minute: Int, second: Int): Int {
        val secondsPassed = (hour * 3600) + (minute * 60) + second
        val dayProgress = secondsPassed / 86400f
        return (baseDailyBurn * dayProgress).toInt()
    }
}
