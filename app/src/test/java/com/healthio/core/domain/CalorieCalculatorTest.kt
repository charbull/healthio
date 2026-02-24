package com.healthio.core.domain

import com.healthio.core.database.WorkoutLog
import com.healthio.core.health.HealthConnectManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class CalorieCalculatorTest {

    private val healthConnectManager = mockk<HealthConnectManager>()
    private val calculator = CalorieCalculator(healthConnectManager)

    @Test
    fun `total burned should be BMR plus manual workout when no HC sync`() = runBlocking {
        // Arrange
        val proRatedBmr = 900
        val manualWorkout = WorkoutLog(timestamp = 0, type = "Run", calories = 300, durationMinutes = 30, source = "Manual")
        
        coEvery { healthConnectManager.hasPermissions() } returns false
        
        // Act
        val result = calculator.calculateTotalBurned(proRatedBmr, listOf(manualWorkout))
        
        // Assert
        assertEquals(1200, result) // 900 + 300
    }

    @Test
    fun `total burned should be BMR plus HC active when no manual workouts`() = runBlocking {
        // Arrange
        val proRatedBmr = 900
        val hcActive = 500
        
        coEvery { healthConnectManager.hasPermissions() } returns true
        coEvery { healthConnectManager.fetchActiveCalories(any(), any()) } returns hcActive
        
        // Act
        val result = calculator.calculateTotalBurned(proRatedBmr, emptyList())
        
        // Assert
        assertEquals(1400, result) // 900 + 500
    }

    @Test
    fun `total burned should override HC calories during manual workout window`() = runBlocking {
        // Arrange
        val proRatedBmr = 1000
        val totalHcActive = 800
        
        val workoutTime = Instant.now().toEpochMilli()
        val manualWorkout = WorkoutLog(
            timestamp = workoutTime,
            type = "Running",
            calories = 500,
            durationMinutes = 60,
            source = "Manual"
        )
        
        // Mock total active for the day
        coEvery { healthConnectManager.hasPermissions() } returns true
        coEvery { healthConnectManager.fetchActiveCalories(any(), any()) } returns totalHcActive
        
        // Mock active calories specifically during the manual workout window (e.g., 300 calories recorded by watch)
        // We need to match the specific window in the calculator logic
        coEvery { healthConnectManager.fetchActiveCalories(match { it.toEpochMilli() == workoutTime }, any()) } returns 300
        
        // Act
        val result = calculator.calculateTotalBurned(proRatedBmr, listOf(manualWorkout))
        
        // Assert
        // Net HC = 800 (total) - 300 (overlap) = 500
        // Total = 1000 (BMR) + 500 (Net HC) + 500 (Manual) = 2000
        assertEquals(2000, result)
    }

    @Test
    fun `total burned should handle multiple manual workouts`() = runBlocking {
        // Arrange
        val proRatedBmr = 1000
        val totalHcActive = 1000
        
        val w1Time = Instant.now().minusSeconds(3600).toEpochMilli()
        val w2Time = Instant.now().toEpochMilli()
        
        val m1 = WorkoutLog(timestamp = w1Time, type = "Run", calories = 400, durationMinutes = 30, source = "Manual")
        val m2 = WorkoutLog(timestamp = w2Time, type = "Lift", calories = 300, durationMinutes = 45, source = "Manual")
        
        coEvery { healthConnectManager.hasPermissions() } returns true
        coEvery { healthConnectManager.fetchActiveCalories(any(), any()) } returns totalHcActive // Default
        coEvery { healthConnectManager.fetchActiveCalories(match { it.toEpochMilli() == w1Time }, any()) } returns 200
        coEvery { healthConnectManager.fetchActiveCalories(match { it.toEpochMilli() == w2Time }, any()) } returns 100
        
        // Act
        val result = calculator.calculateTotalBurned(proRatedBmr, listOf(m1, m2))
        
        // Assert
        // Net HC = 1000 - 200 - 100 = 700
        // Total = 1000 (BMR) + 700 (Net HC) + 400 (M1) + 300 (M2) = 2400
        assertEquals(2400, result)
    }
}
