# Track 08: Fix Fasting Stats

**Goal:** Fix the issue where month fasting stats are not working correctly, likely due to incorrect aggregation logic.

## Scope
1.  **Fasting Statistics Logic:**
    -   Update `StatsViewModel` to accumulate fasting duration per day (`sum`) instead of taking the maximum single segment (`maxOf`).
    -   This ensures that if a fast spans across midnight, or if there are multiple fasts in a day, the total hours are correctly reported.
2.  **Verification:**
    -   Verify that the change applies to all time ranges (Week, Month, Year).

## Success Criteria
-   The "Fasting Time (Hours)" chart correctly displays the *total* hours fasted for each day/bucket.
