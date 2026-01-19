# Implementation Plan - Track 14: Fix Fasting Week/Year Stats

- [ ] Update `app/src/test/java/com/healthio/ui/stats/StatsUtilsTest.kt`:
    - [ ] Add tests for `TimeRange.Week`:
        - [ ] Monday of current week.
        - [ ] Sunday of current week.
        - [ ] Sunday of *previous* week (should fail).
        - [ ] Monday of *next* week (should fail).
    - [ ] Add tests for `TimeRange.Year`:
        - [ ] Jan 1st of current year.
        - [ ] Dec 31st of current year.
        - [ ] Dec 31st of *previous* year (should fail).
- [ ] Run tests and identify failures.
- [ ] Fix logic in `StatsUtils.kt` if necessary.
- [ ] Verify `StatsViewModel` isn't filtering incorrectly before calling `StatsUtils`.
