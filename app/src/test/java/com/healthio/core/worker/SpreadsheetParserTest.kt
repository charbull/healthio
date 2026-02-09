package com.healthio.core.worker

import org.junit.Assert.*
import org.junit.Test

class SpreadsheetParserTest {

    @Test
    fun `parseFastingRow - should correctly parse valid row`() {
        // Date, Start, End, Hours, RawTimestamp
        val row = listOf("2026-02-05", "08:00:00", "16:00:00", "8.0", "1738742400000")
        val timestampIdx = 4
        
        val result = SpreadsheetParser.parseFastingRow(row, timestampIdx)
        
        assertNotNull(result)
        assertEquals(1738742400000L, result?.startTime)
        assertEquals(1738742400000L + 8 * 3600000L, result?.endTime)
        assertEquals(8 * 3600000L, result?.durationMillis)
        assertTrue(result?.isSynced ?: false)
    }

    @Test
    fun `parseMealRow - should correctly parse valid row`() {
        // Date, Time, Food, Calories, Protein, Carbs, Fat, RawTimestamp
        val row = listOf("2026-02-05", "12:00:00", "Steak", "500", "40", "5", "30", "1738756800000")
        val timestampIdx = 7
        
        val result = SpreadsheetParser.parseMealRow(row, timestampIdx)
        
        assertNotNull(result)
        assertEquals(1738756800000L, result?.timestamp)
        assertEquals("Steak", result?.foodName)
        assertEquals(500, result?.calories)
        assertEquals(40, result?.protein)
        assertEquals(5, result?.carbs)
        assertEquals(30, result?.fat)
        assertTrue(result?.isSynced ?: false)
    }

    @Test
    fun `parseWorkoutRow - should correctly parse valid row`() {
        // Date, Time, Type, Calories, DurationMin, RawTimestamp
        val row = listOf("2026-02-05", "18:00:00", "Running", "600", "45", "1738778400000")
        val timestampIdx = 5
        
        val result = SpreadsheetParser.parseWorkoutRow(row, timestampIdx)
        
        assertNotNull(result)
        assertEquals(1738778400000L, result?.timestamp)
        assertEquals("Running", result?.type)
        assertEquals(600, result?.calories)
        assertEquals(45, result?.durationMinutes)
        assertTrue(result?.isSynced ?: false)
    }

    @Test
    fun `parseWeightRow - should correctly parse valid row`() {
        // Date, Time, WeightKg, RawTimestamp
        val row = listOf("2026-02-05", "07:00:00", "75.5", "1738738800000")
        val timestampIdx = 3
        
        val result = SpreadsheetParser.parseWeightRow(row, timestampIdx)
        
        assertNotNull(result)
        assertEquals(1738738800000L, result?.timestamp)
        assertEquals(75.5f, result?.valueKg ?: 0f, 0.01f)
        assertTrue(result?.isSynced ?: false)
    }

    @Test
    fun `parser methods - should return null on invalid timestamp`() {
        val row = listOf("invalid", "invalid", "invalid", "not_a_number")
        assertNull(SpreadsheetParser.parseFastingRow(row, 3))
        assertNull(SpreadsheetParser.parseMealRow(row, 3))
        assertNull(SpreadsheetParser.parseWorkoutRow(row, 3))
        assertNull(SpreadsheetParser.parseWeightRow(row, 3))
    }
}
