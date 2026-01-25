package com.healthio.ui.stats

import com.healthio.core.database.WeightLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class WeightSeriesCalculatorTest {

    @Test
    fun `calculate - correctly maps weights and carries forward history`() {
        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 1, 25) // Sunday
        
        // History: Jan 10 (Saturday, 2 weeks prior) -> 190 lbs = 86.1825 kg
        val historyLog = WeightLog(
            timestamp = LocalDate.of(2026, 1, 10).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            valueKg = 86.1825f,
            source = "test"
        )
        
        // Recent: Jan 24 (Saturday) -> 198 lbs = 89.8112 kg
        val satLog = WeightLog(
            timestamp = LocalDate.of(2026, 1, 24).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            valueKg = 89.8112f,
            source = "test"
        )

        // Today: Jan 25 (Sunday) -> 196 lbs = 88.9041 kg
        val sunLog = WeightLog(
            timestamp = LocalDate.of(2026, 1, 25).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            valueKg = 88.9041f,
            source = "test"
        )

        val logs = listOf(historyLog, satLog, sunLog)
        
        val result = WeightSeriesCalculator.calculate(
            allWeightLogs = logs,
            range = TimeRange.Week,
            today = today,
            zoneId = zoneId
        )
        
        // Verify we have 7 entries (Mon-Sun)
        assertEquals("Should have 7 entries", 7, result.size)
        
        // Mon (0) - Fri (4) should match history (190)
        // 86.1825 * 2.20462 = 189.999... -> 190
        for (i in 0..4) {
            assertEquals("Day $i should be ~190", 190f, result[i].y, 0.1f)
        }
        
        // Sat (5) should be 198
        assertEquals("Day 5 (Sat) should be ~198", 198f, result[5].y, 0.1f)
        
        // Sun (6) should be 196
        assertEquals("Day 6 (Sun) should be ~196", 196f, result[6].y, 0.1f)
    }
}
