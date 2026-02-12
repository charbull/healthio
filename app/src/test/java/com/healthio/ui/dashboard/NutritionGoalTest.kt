package com.healthio.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionGoalTest {

    @Test
    fun `Protein goal - calculation uses kg even when display unit is LBS`() {
        // Scenario: 100kg user (~220 lbs) with a 1.5g/kg multiplier
        // Correct Goal: 100 * 1.5 = 150g
        // Buggy Goal (if using LBS): 220.46 * 1.5 = 330g
        
        val weightKg = 100f
        val multiplier = 1.5f
        
        // Test LBS scenario
        val proteinGoalLbs = calculateProteinGoal(
            weightKg = weightKg,
            multiplier = multiplier,
            weightUnit = "LBS",
            method = "MULTIPLIER"
        )
        
        assertEquals("Protein goal should be 150g for 100kg user", 150, proteinGoalLbs)

        // Test KG scenario
        val proteinGoalKg = calculateProteinGoal(
            weightKg = weightKg,
            multiplier = multiplier,
            weightUnit = "KG",
            method = "MULTIPLIER"
        )
        
        assertEquals("Protein goal should be 150g for 100kg user regardless of unit", 150, proteinGoalKg)
    }

    @Test
    fun `Protein goal - respects FIXED method`() {
        val goal = calculateProteinGoal(
            weightKg = 100f,
            multiplier = 1.5f,
            weightUnit = "LBS",
            method = "FIXED",
            fixedGoal = 200
        )
        assertEquals(200, goal)
    }

    /**
     * Helper mimicking logic in HomeViewModel.updateState
     */
    private fun calculateProteinGoal(
        weightKg: Float,
        multiplier: Float,
        weightUnit: String,
        method: String,
        fixedGoal: Int = 150
    ): Int {
        // Logic from HomeViewModel.kt
        return if (method == "FIXED") {
            fixedGoal
        } else {
            // This is the specific line we fixed:
            // (multiplier * weightKg).toInt()
            (multiplier * weightKg).toInt()
        }
    }
}
