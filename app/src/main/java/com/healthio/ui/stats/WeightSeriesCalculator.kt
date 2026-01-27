package com.healthio.ui.stats

import com.healthio.core.database.WeightLog
import com.patrykandpatrick.vico.core.entry.ChartEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object WeightSeriesCalculator {
    fun calculate(
        allWeightLogs: List<WeightLog>,
        range: TimeRange,
        today: LocalDate,
        zoneId: ZoneId
    ): List<ChartEntry> {
        val sortedWeights = allWeightLogs.sortedBy { it.timestamp }
        val weightMap = mutableMapOf<Int, Float>()
        var lastKnownWeightKg: Float? = null
        
        val bucketCount = when (range) {
            TimeRange.Week -> 7
            TimeRange.Month -> today.lengthOfMonth()
            TimeRange.Year -> 12
        }

        val chartStartDate = when (range) {
            TimeRange.Week -> today.minusDays(6)
            TimeRange.Month -> today.withDayOfMonth(1)
            TimeRange.Year -> today.withDayOfYear(1)
        }
        
        // 1. Determine initial weight and map daily weights
        sortedWeights.forEach { log ->
            val logDate = Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate()
            
            if (logDate.isBefore(chartStartDate)) {
                lastKnownWeightKg = log.valueKg
            } else {
                val index = when (range) {
                    TimeRange.Week -> ChronoUnit.DAYS.between(chartStartDate, logDate).toInt() + 1
                    TimeRange.Month -> ChronoUnit.DAYS.between(chartStartDate, logDate).toInt() + 1
                    TimeRange.Year -> if (logDate.year == today.year) logDate.monthValue else -1
                }

                if (index in 1..bucketCount) {
                    // If multiple logs on same day, take the latest one (sorted order helps)
                    weightMap[index] = log.valueKg
                }
            }
        }
        
        val weightEntries = mutableListOf<ChartEntry>()
        
        // 2. Build series with carry-forward
        for (i in 1..bucketCount) {
            val dailyWeight = weightMap[i]
            if (dailyWeight != null) {
                lastKnownWeightKg = dailyWeight
            }
            
            if (lastKnownWeightKg != null) {
                val weightLbs = lastKnownWeightKg!! * 2.20462f
                weightEntries.add(entryOf(i - 1, weightLbs))
            }
        }
        
        return weightEntries
    }
}
