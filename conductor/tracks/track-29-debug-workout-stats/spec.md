# Track 29: Debug Universal Stats & X-Axis

**Goal:** Fix the critical bug where Week and Year views are empty for ALL stats (Fasting, Workouts, Calories) and correct the Month view X-axis labels.

## Scope
1.  **Empty Week/Year Views:**
    -   Investigate why `StatsUtils` filtering or `StatsViewModel` aggregation is failing for Week and Year ranges, despite unit tests passing.
    -   Hypothesis: Is the `today` variable in `StatsViewModel` consistent with the logs?
    -   Hypothesis: Is the `entryOf` X-value logic (`it - 1`) misaligned with the `labels` list index?
2.  **Month View X-Axis:**
    -   Fix missing labels.
    -   Ensure labels match the days (1..31).
3.  **Chart Artifacts:**
    -   Investigate the "extra line" report.

## Success Criteria
-   Week view shows bars for Fasting, Workouts, Calories.
-   Year view shows bars for Fasting, Workouts, Calories.
-   Month view shows correct X-axis labels (days).
