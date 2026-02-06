package com.healthio.core.health

import com.healthio.core.database.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthSyncUtilsTest {

    @Test
    fun `calculateAdjustedActiveBurn - should subtract workout calories`() {
        val totalActive = 1000
        val workouts = listOf(
            WorkoutLog(id = 1, timestamp = 0, type = "Run", calories = 300, durationMinutes = 30, source = "HC", externalId = "run1"),
            WorkoutLog(id = 2, timestamp = 0, type = "Walk", calories = 100, durationMinutes = 20, source = "HC", externalId = "walk1")
        )
        val activeBurnId = "daily_burn"

        val result = HealthSyncUtils.calculateAdjustedActiveBurn(totalActive, workouts, activeBurnId)
        
        // 1000 - (300 + 100) = 600
        assertEquals(600, result)
    }

    @Test
    fun `calculateAdjustedActiveBurn - should ignore the active burn record itself`() {
        val totalActive = 1000
        val activeBurnId = "daily_burn"
        val workouts = listOf(
            WorkoutLog(id = 1, timestamp = 0, type = "Run", calories = 300, durationMinutes = 30, source = "HC", externalId = "run1"),
            // This represents an existing active burn record that we are about to update/replace
            WorkoutLog(id = 3, timestamp = 0, type = "Daily", calories = 500, durationMinutes = 0, source = "HC", externalId = activeBurnId)
        )

        val result = HealthSyncUtils.calculateAdjustedActiveBurn(totalActive, workouts, activeBurnId)
        
        // 1000 - 300 = 700 (it should NOT subtract the 500 from the daily record itself)
        assertEquals(700, result)
    }

    @Test
    fun `calculateAdjustedActiveBurn - should not return negative values`() {
        val totalActive = 200
        val workouts = listOf(
            WorkoutLog(id = 1, timestamp = 0, type = "Run", calories = 500, durationMinutes = 30, source = "HC", externalId = "run1")
        )
        val activeBurnId = "daily_burn"

        val result = HealthSyncUtils.calculateAdjustedActiveBurn(totalActive, workouts, activeBurnId)
        
        // 200 - 500 = -300 -> 0
        assertEquals(0, result)
    }

    @Test
    fun `calculateAdjustedActiveBurn - should handle empty workouts`() {
        val result = HealthSyncUtils.calculateAdjustedActiveBurn(500, emptyList(), "any")
        assertEquals(500, result)
    }
}
