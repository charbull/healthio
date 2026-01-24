package com.healthio.ui.stats

import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class StatsSplitTest {

    @Test
    fun `Test Fasting Split Across Midnight`() {
        val zoneId = ZoneId.of("UTC") // Fixed zone for testing
        val today = LocalDate.of(2026, 1, 23) // Friday
        
        // Start: Thursday 8 PM
        val startDateTime = LocalDateTime.of(2026, 1, 22, 20, 0)
        val startTime = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        
        // End: Friday 4 PM
        val endDateTime = LocalDateTime.of(2026, 1, 23, 16, 0)
        val endTime = endDateTime.atZone(zoneId).toInstant().toEpochMilli()
        
        println("Start: $startDateTime")
        println("End: $endDateTime")
        
        // Logic from StatsViewModel
        val dailyTotal = mutableMapOf<Int, Float>()
        val range = TimeRange.Week
        
        var currentStart = java.time.Instant.ofEpochMilli(startTime).atZone(zoneId)
        val endInstant = java.time.Instant.ofEpochMilli(endTime).atZone(zoneId)
        
        while (currentStart.isBefore(endInstant)) {
            val endOfDay = currentStart.toLocalDate().plusDays(1).atStartOfDay(zoneId)
            val segmentEnd = if (endInstant.isBefore(endOfDay)) endInstant else endOfDay
            
            val (index, include) = StatsUtils.getBucketIndex(currentStart.toLocalDate(), range, today)
            
            println("Segment: ${currentStart.toLocalDateTime()} to ${segmentEnd.toLocalDateTime()} -> Index $index, Include $include")
            
            if (include) {
                val durationHrs = ChronoUnit.MILLIS.between(currentStart, segmentEnd) / 3600000f
                dailyTotal[index] = (dailyTotal[index] ?: 0f) + durationHrs
            }
            currentStart = segmentEnd
        }
        
        println("Results: $dailyTotal")
        
        // Thursday (Day 4) should have 4 hours
        // Friday (Day 5) should have 16 hours
        
        assert(dailyTotal[4] == 4.0f) { "Thursday should be 4.0" }
        assert(dailyTotal[5] == 16.0f) { "Friday should be 16.0" }
    }
}
