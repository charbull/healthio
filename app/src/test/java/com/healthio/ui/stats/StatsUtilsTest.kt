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
    fun `getBucketIndex Week - boundaries check`() {
        // Jan 19, 2026 is Monday
        val today = LocalDate.of(2026, 1, 19)
        
        // Monday (Start of week)
        val mon = LocalDate.of(2026, 1, 19)
        val (idxMon, incMon) = StatsUtils.getBucketIndex(mon, TimeRange.Week, today)
        assertEquals(1, idxMon)
        assertTrue("Monday should be included", incMon)

        // Sunday (End of week)
        val sun = LocalDate.of(2026, 1, 25)
        val (idxSun, incSun) = StatsUtils.getBucketIndex(sun, TimeRange.Week, today)
        assertEquals(7, idxSun)
        assertTrue("Sunday should be included", incSun)

        // Previous Sunday (Excluded)
        val prevSun = LocalDate.of(2026, 1, 18)
        val (_, incPrevSun) = StatsUtils.getBucketIndex(prevSun, TimeRange.Week, today)
        assertFalse("Previous Sunday should be excluded", incPrevSun)

        // Next Monday (Excluded)
        val nextMon = LocalDate.of(2026, 1, 26)
        val (_, incNextMon) = StatsUtils.getBucketIndex(nextMon, TimeRange.Week, today)
        assertFalse("Next Monday should be excluded", incNextMon)
    }

    @Test
    fun `getBucketIndex Year - boundaries check`() {
        val today = LocalDate.of(2026, 6, 15)

        // Jan 1st
        val jan1 = LocalDate.of(2026, 1, 1)
        val (idxJan, incJan) = StatsUtils.getBucketIndex(jan1, TimeRange.Year, today)
        assertEquals(1, idxJan)
        assertTrue("Jan 1st should be included", incJan)

        // Dec 31st
        val dec31 = LocalDate.of(2026, 12, 31)
        val (idxDec, incDec) = StatsUtils.getBucketIndex(dec31, TimeRange.Year, today)
        assertEquals(12, idxDec)
        assertTrue("Dec 31st should be included", incDec)

        // Prev Year Dec 31st
        val prevDec = LocalDate.of(2025, 12, 31)
        val (_, incPrev) = StatsUtils.getBucketIndex(prevDec, TimeRange.Year, today)
        assertFalse("Previous year should be excluded", incPrev)

        // Next Year Jan 1st
        val nextJan = LocalDate.of(2027, 1, 1)
        val (_, incNext) = StatsUtils.getBucketIndex(nextJan, TimeRange.Year, today)
        assertFalse("Next year should be excluded", incNext)
    }

    @Test
    fun `Regression - Current time is always in current week`() {
        // This test uses the actual system time to ensure no timezone math weirdness affects "Now"
        val nowMillis = System.currentTimeMillis()
        val zoneId = java.time.ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val date = java.time.Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        
        val (_, include) = StatsUtils.getBucketIndex(date, TimeRange.Week, today)
        
        assertTrue("System.currentTimeMillis() MUST be included in the current Week view", include)
    }
}
