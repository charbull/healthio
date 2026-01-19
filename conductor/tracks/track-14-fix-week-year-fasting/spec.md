# Track 14: Fix Fasting Week/Year Stats

**Goal:** Investigate and fix the reported issue where "Fasting Week and Year are broken", while ensuring Month stats remain correct.

## Scope
1.  **Investigation:**
    -   Review `StatsUtils.kt` logic for `TimeRange.Week` and `TimeRange.Year`.
    -   Create comprehensive unit tests covering boundary conditions and edge cases.
2.  **Fix:**
    -   Address any logical errors identified by the tests.
    -   Ensure `StatsViewModel` usage of `StatsUtils` is consistent.

## Success Criteria
-   Unit tests verify correct behavior for Week (Mon-Sun of current week) and Year (Jan-Dec of current year).
-   User confirms Week and Year stats are working.
