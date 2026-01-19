# Track 11: Unit Tests for Stats Logic

**Goal:** Implement unit tests to ensure the statistics aggregation and bucket logic (Week, Month, Year) is correct and remains stable.

## Scope
1.  **Refactor for Testability:**
    -   Extract the date-to-bucket logic from `StatsViewModel` into a testable utility (e.g., `StatsUtils.kt`).
2.  **Unit Tests:**
    -   Test `getBucketIndex` for various edge cases (leap years, month boundaries, year changes).
    -   Test that `TimeRange.Month` correctly identifies dates within the current month/year.
    -   Test that `TimeRange.Week` correctly identifies dates within the current week (Mon-Sun).

## Success Criteria
-   Unit tests pass for all time ranges.
-   Code is refactored to be more modular and testable.
