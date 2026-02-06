package com.healthio.core.health

import com.healthio.core.database.WorkoutLog

object HealthSyncUtils {
    /**
     * Calculates the adjusted active burn by subtracting individual workout calories
     * from the total active calories reported by Health Connect.
     */
    fun calculateAdjustedActiveBurn(
        totalActiveCalories: Int,
        workouts: List<WorkoutLog>,
        activeBurnExternalId: String
    ): Int {
        val workoutCaloriesSum = workouts
            .filter { it.externalId != activeBurnExternalId }
            .sumOf { it.calories }

        return (totalActiveCalories - workoutCaloriesSum).coerceAtLeast(0)
    }
}
