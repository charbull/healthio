package com.healthio.core.domain

import com.healthio.core.database.WorkoutLog
import com.healthio.core.health.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CalorieCalculator(
    private val healthConnectManager: HealthConnectManager
) {
    /**
     * Calculates the total burned calories for the current day.
     * Logic:
     * 1. Get total Active Calories Burned from Health Connect for today.
     * 2. Identify Manual Workouts logged in Healthio for today.
     * 3. For each manual workout, find how many HC calories were recorded during its window.
     * 4. Total = (Total HC Active) - (Overlapping HC calories) + (Manual Workout Calories)
     */
    suspend fun calculateTotalBurned(
        proRatedBmr: Int,
        workouts: List<WorkoutLog>
    ): Int {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zoneId).toInstant()

        // 1. Check for Health Connect Sync
        if (!healthConnectManager.hasPermissions()) {
            // No sync: Fallback to manual workouts + proRatedBmr
            val totalManualCalories = workouts.filter { it.source == "Manual" }.sumOf { it.calories }
            return proRatedBmr + totalManualCalories
        }

        // 2. Total HC Active Calories for the day
        val totalHcActive = healthConnectManager.fetchActiveCalories(startOfDay, endOfDay)

        // 3. Identify Manual Workouts
        val manualWorkouts = workouts.filter { it.source == "Manual" }
        
        // 4. Calculate overlap
        var hcOverlapCalories = 0
        var totalManualCalories = 0
        
        for (workout in manualWorkouts) {
            val startTime = Instant.ofEpochMilli(workout.timestamp)
            val endTime = startTime.plusMillis(workout.durationMinutes.toLong() * 60 * 1000)
            
            // Query HC specifically for this window to see what it recorded
            val hcCaloriesDuringWorkout = healthConnectManager.fetchActiveCalories(startTime, endTime)
            hcOverlapCalories += hcCaloriesDuringWorkout
            totalManualCalories += workout.calories
        }

        // 5. Final Aggregation
        // If HC Active is 0, we might still want to consider other imported workouts if they exist
        // But per requirements, we prioritize HC Active if present.
        val netHcActive = (totalHcActive - hcOverlapCalories).coerceAtLeast(0)
        
        return proRatedBmr + netHcActive + totalManualCalories
    }
}
