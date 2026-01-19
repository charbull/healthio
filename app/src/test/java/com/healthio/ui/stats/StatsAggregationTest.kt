package com.healthio.ui.stats

import com.healthio.core.database.MealLog
import com.healthio.core.database.WorkoutLog
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StatsAggregationTest {
    
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun `Energy Intake - sums calories per day correctly (Week)`() {
        val today = LocalDate.of(2026, 1, 19) // Monday
        val range = TimeRange.Week
        
        // Mon: 500 kcal
        val monMeal = MealLog(
            timestamp = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
            foodName = "Test", calories = 500, protein = 0, carbs = 0, fat = 0
        )
        // Mon: 200 kcal
        val monMeal2 = MealLog(
            timestamp = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
            foodName = "Test", calories = 200, protein = 0, carbs = 0, fat = 0
        )
        // Tue: 300 kcal
        val tueMeal = MealLog(
            timestamp = today.plusDays(1).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
            foodName = "Test", calories = 300, protein = 0, carbs = 0, fat = 0
        )
        
        val logs = listOf(monMeal, monMeal2, tueMeal)
        val aggregated = aggregateCalories(logs, range, today)
        
        assertEquals(700f, aggregated[1] ?: 0f, 0.01f) // Mon
        assertEquals(300f, aggregated[2] ?: 0f, 0.01f) // Tue
    }

    @Test
    fun `Macros - sums nutrients per day correctly (Week)`() {
        val today = LocalDate.of(2026, 1, 19) // Monday
        val range = TimeRange.Week
        
        // Mon: 20g P, 30g C, 10g F
        val monMeal = MealLog(
            timestamp = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
            foodName = "Test", calories = 0, protein = 20, carbs = 30, fat = 10
        )
        // Mon: 10g P, 10g C, 5g F
        val monMeal2 = MealLog(
            timestamp = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
            foodName = "Test", calories = 0, protein = 10, carbs = 10, fat = 5
        )
        
        val logs = listOf(monMeal, monMeal2)
        val (p, c, f) = aggregateMacros(logs, range, today)
        
        assertEquals(30f, p[1] ?: 0f, 0.01f) // Protein
        assertEquals(40f, c[1] ?: 0f, 0.01f) // Carbs
        assertEquals(15f, f[1] ?: 0f, 0.01f) // Fat
    }

    @Test
    fun `Energy Burned - sums workout calories correctly (Year)`() {
        val today = LocalDate.of(2026, 6, 15)
        val range = TimeRange.Year
        
        // Jan: 500 kcal
        val janWorkout = WorkoutLog(
            timestamp = LocalDate.of(2026, 1, 10).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = "Run", calories = 500, durationMinutes = 30, source = "Manual"
        )
        // Jan: 200 kcal
        val janWorkout2 = WorkoutLog(
            timestamp = LocalDate.of(2026, 1, 20).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = "Run", calories = 200, durationMinutes = 30, source = "Manual"
        )
        // Feb: 300 kcal
        val febWorkout = WorkoutLog(
            timestamp = LocalDate.of(2026, 2, 10).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            type = "Run", calories = 300, durationMinutes = 30, source = "Manual"
        )
        
        val logs = listOf(janWorkout, janWorkout2, febWorkout)
        val aggregated = aggregateWorkoutCalories(logs, range, today)
        
        assertEquals(700f, aggregated[1] ?: 0f, 0.01f) // Jan
        assertEquals(300f, aggregated[2] ?: 0f, 0.01f) // Feb
    }

    // Logic helpers mimicking ViewModel
    private fun aggregateCalories(logs: List<MealLog>, range: TimeRange, today: LocalDate): Map<Int, Float> {
        val map = mutableMapOf<Int, Float>()
        logs.filter { 
            StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), range, today).second 
        }.forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            map[index] = (map[index] ?: 0f) + log.calories.toFloat()
        }
        return map
    }
    
    private fun aggregateMacros(logs: List<MealLog>, range: TimeRange, today: LocalDate): Triple<Map<Int, Float>, Map<Int, Float>, Map<Int, Float>> {
        val p = mutableMapOf<Int, Float>()
        val c = mutableMapOf<Int, Float>()
        val f = mutableMapOf<Int, Float>()
        
        logs.filter { 
            StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), range, today).second 
        }.forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            p[index] = (p[index] ?: 0f) + log.protein.toFloat()
            c[index] = (c[index] ?: 0f) + log.carbs.toFloat()
            f[index] = (f[index] ?: 0f) + log.fat.toFloat()
        }
        return Triple(p, c, f)
    }

    private fun aggregateWorkoutCalories(logs: List<WorkoutLog>, range: TimeRange, today: LocalDate): Map<Int, Float> {
        val map = mutableMapOf<Int, Float>()
        logs.filter { 
            StatsUtils.getBucketIndex(Instant.ofEpochMilli(it.timestamp).atZone(zoneId).toLocalDate(), range, today).second 
        }.forEach { log ->
            val (index, _) = StatsUtils.getBucketIndex(Instant.ofEpochMilli(log.timestamp).atZone(zoneId).toLocalDate(), range, today)
            map[index] = (map[index] ?: 0f) + log.calories.toFloat()
        }
        return map
    }
}
