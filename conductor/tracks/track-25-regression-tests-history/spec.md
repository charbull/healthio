# Track 25: Regression Tests for History

**Goal:** Permanently add regression tests to ensure that current timestamps ("Now") are correctly bucketed in the Week view, preventing future regressions where manual entries might disappear.

## Scope
1.  **Update `StatsUtilsTest.kt`:**
    -   Add a test case using `System.currentTimeMillis()` (or equivalent dynamic check) to assert that "Today" is always included in the "Week" range.
    -   Add a test case for edge cases like 11:59 PM Sunday and 12:01 AM Monday.

## Success Criteria
-   `StatsUtilsTest` includes explicit "Real Time" or "Boundary" checks that guarantee data validity for the current moment.
