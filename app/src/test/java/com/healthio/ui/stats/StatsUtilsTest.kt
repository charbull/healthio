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
        
        // Today (Monday) should be index 7
        val (idxMon, incMon) = StatsUtils.getBucketIndex(today, TimeRange.Week, today)
        assertEquals(7, idxMon)
        assertTrue("Today should be included as index 7", incMon)

        // 6 Days Ago (Tuesday Jan 13) should be index 1
        val tues = LocalDate.of(2026, 1, 13)
        val (idxTues, incTues) = StatsUtils.getBucketIndex(tues, TimeRange.Week, today)
        assertEquals(1, idxTues)
        assertTrue("6 days ago should be index 1", incTues)

        // Previous Monday (Excluded)
        val prevMon = LocalDate.of(2026, 1, 12)
        val (_, incPrevMon) = StatsUtils.getBucketIndex(prevMon, TimeRange.Week, today)
        assertFalse("7 days ago should be excluded", incPrevMon)

        // Tomorrow (Excluded)
        val tomorrow = LocalDate.of(2026, 1, 20)
        val (_, incTomorrow) = StatsUtils.getBucketIndex(tomorrow, TimeRange.Week, today)
        assertFalse("Future dates should be excluded", incTomorrow)
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

    @Test
    fun `calculateProRatedBMR - start of day`() {
        val bmr = StatsUtils.calculateProRatedBMR(2400, 0, 0, 0)
        assertEquals(0, bmr)
    }

    @Test
    fun `calculateProRatedBMR - noon`() {
        val bmr = StatsUtils.calculateProRatedBMR(2400, 12, 0, 0)
        assertEquals(1200, bmr)
    }

    @Test
    fun `calculateProRatedBMR - end of day`() {
        // 23:59:59
        val bmr = StatsUtils.calculateProRatedBMR(2400, 23, 59, 59)
        assertTrue("Should be close to 2400", bmr >= 2399)
    }

    @Test
    fun `calculateProRatedBMR - rounding check`() {
        // 1 hour passed = 1/24 of day. 2400 / 24 = 100
        val bmr = StatsUtils.calculateProRatedBMR(2400, 1, 0, 0)
        assertEquals(100, bmr)
    }

    @Test
    fun `calculateProRatedBMR - precision check`() {
        // 1 second passed. 86400 / 86400 = 1.0. 
        // 86400 * (1/86400) = 1
        val bmr = StatsUtils.calculateProRatedBMR(86400, 0, 0, 1)
        assertEquals(1, bmr)
    }
}
