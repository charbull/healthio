package com.healthio.core.ai

import org.junit.Assert.*
import org.junit.Test

class GeminiParserTest {

    private val repository = GeminiRepository()

    @Test
    fun `parseResponse - clean JSON`() {
        val rawText = """{"foodName": "Apple", "calories": 95, "protein": 0, "carbs": 25, "fat": 0, "fiber": 4, "insulinScore": 10, "healthScore": 10, "feedback": "Healthy"}"""
        val result = repository.parseResponse(rawText)
        
        assertTrue(result.isSuccess)
        assertEquals("Apple", result.getOrNull()?.foodName)
        assertEquals(95.0, result.getOrNull()?.calories ?: 0.0, 0.1)
    }

    @Test
    fun `parseResponse - JSON with Markdown`() {
        val rawText = """Here is your analysis: ```json {"foodName": "Pizza", "calories": 500, "protein": 20, "carbs": 60, "fat": 20, "fiber": 2, "insulinScore": 70, "healthScore": 4, "feedback": "Moderate"} ``` Enjoy!"""
        val result = repository.parseResponse(rawText)
        
        assertTrue(result.isSuccess)
        assertEquals("Pizza", result.getOrNull()?.foodName)
        assertEquals(500.0, result.getOrNull()?.calories ?: 0.0, 0.1)
    }

    @Test
    fun `parseResponse - malformed JSON`() {
        val rawText = "This is not JSON at all."
        val result = repository.parseResponse(rawText)
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `parseResponse - incomplete JSON`() {
        val rawText = """{"foodName": "Steak", "calories": 300"""
        val result = repository.parseResponse(rawText)
        
        assertTrue(result.isFailure)
    }

    @Test
    fun `parseResponse - handles decimals`() {
        // This is the case that was failing
        val rawText = """{"foodName": "Egg", "calories": 70, "protein": 6.5, "carbs": 0.5, "fat": 5, "fiber": 0, "insulinScore": 5, "healthScore": 9, "feedback": "Good"}"""
        val result = repository.parseResponse(rawText)
        
        assertTrue("Should successfully parse even with decimals", result.isSuccess)
        assertEquals(6.5, result.getOrNull()?.protein ?: 0.0, 0.01)
        assertEquals(0.5, result.getOrNull()?.carbs ?: 0.0, 0.01)
    }
}
