package com.healthio.ui.stats

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StatsUtilsTest {

    @Test
    fun `getBucketIndex Month - same month and year should be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2026, 1, 5)
        
        val (index, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Month, today)
        
        assertEquals(5, index)
        assertTrue(include)
    }

    @Test
    fun `getBucketIndex Month - different month should not be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2025, 12, 19)
        
        val (_, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Month, today)
        
        assertFalse(include)
    }

    @Test
    fun `getBucketIndex Month - different year should not be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2025, 1, 19)
        
        val (_, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Month, today)
        
        assertFalse(include)
    }

    @Test
    fun `getBucketIndex Week - same week should be included`() {
        // Jan 19, 2026 is Monday
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2026, 1, 21) // Wednesday
        
        val (index, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Week, today)
        
        assertEquals(3, index) // Wednesday is 3
        assertTrue(include)
    }

    @Test
    fun `getBucketIndex Week - previous week should not be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2026, 1, 18) // Sunday
        
        val (_, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Week, today)
        
        assertFalse(include)
    }

    @Test
    fun `getBucketIndex Year - same year should be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2026, 10, 5)
        
        val (index, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Year, today)
        
        assertEquals(10, index) // October is 10
        assertTrue(include)
    }

    @Test
    fun `getBucketIndex Year - different year should not be included`() {
        val today = LocalDate.of(2026, 1, 19)
        val testDate = LocalDate.of(2025, 1, 19)
        
        val (_, include) = StatsUtils.getBucketIndex(testDate, TimeRange.Year, today)
        
        assertFalse(include)
    }
}
