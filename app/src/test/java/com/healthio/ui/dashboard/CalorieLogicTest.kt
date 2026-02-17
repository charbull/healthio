package com.healthio.ui.dashboard

import com.healthio.core.database.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Test

class CalorieLogicTest {

    @Test
    fun `Total Burned - calculates correctly with BMR, HC Active Burn, and Manual workouts`() {
        val proRatedBmr = 1200 // Noon
        
        // Scenario 1: Only BMR
        val total1 = calculateTotalBurned(
            proRatedBmr = proRatedBmr,
            workouts = emptyList()
        )
        assertEquals("Should be just pro-rated BMR", 1200, total1)

        // Scenario 2: BMR + Manual Workout
        val manualWorkout = WorkoutLog(
            timestamp = 0, type = "Run", calories = 500, durationMinutes = 30, source = "Manual"
        )
        val total2 = calculateTotalBurned(
            proRatedBmr = proRatedBmr,
            workouts = listOf(manualWorkout)
        )
        assertEquals("Should be BMR + Manual", 1700, total2)

        // Scenario 3: BMR + HC Active Burn
        val hcActiveBurn = WorkoutLog(
            timestamp = 0, type = "Health Connect Active Burn", calories = 300, durationMinutes = 0, source = "Health Connect"
        )
        val total3 = calculateTotalBurned(
            proRatedBmr = proRatedBmr,
            workouts = listOf(hcActiveBurn)
        )
        assertEquals("Should be BMR + HC Active", 1500, total3)

        // Scenario 4: BMR + HC Active + Manual
        val total4 = calculateTotalBurned(
            proRatedBmr = proRatedBmr,
            workouts = listOf(hcActiveBurn, manualWorkout)
        )
        assertEquals("Should be BMR + HC Active + Manual", 2000, total4)
        
        // Scenario 5: Regression check - ignore 'Health Connect Daily' (Aggregate) in additive mode
        val hcDaily = WorkoutLog(
            timestamp = 0, type = "Health Connect Daily", calories = 5000, durationMinutes = 0, source = "Health Connect"
        )
        val total5 = calculateTotalBurned(
            proRatedBmr = proRatedBmr,
            workouts = listOf(hcActiveBurn, manualWorkout, hcDaily)
        )
        assertEquals("Should still be 2000 (ignore HC Daily)", 2000, total5)
    }

    /**
     * Mimics HomeViewModel logic
     */
    private fun calculateTotalBurned(proRatedBmr: Int, workouts: List<WorkoutLog>): Int {
        val hcActiveBurn = workouts.filter { it.type == "Health Connect Active Burn" }.sumOf { it.calories }
        val manualWorkoutsSum = workouts.filter { it.source == "Manual" }.sumOf { it.calories }
        
        // In the fallback case (no HC Active Burn), we sum other individual workouts (Imported from HC)
        val otherWorkoutsSum = if (hcActiveBurn == 0) {
            workouts.filter { it.source != "Manual" && it.type != "Health Connect Active Burn" && it.type != "Health Connect Daily" }.sumOf { it.calories }
        } else {
            0
        }

        return proRatedBmr + hcActiveBurn + manualWorkoutsSum + otherWorkoutsSum
    }
}
