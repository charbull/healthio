# Track 28: Chart & Stats Polish

**Goal:** Fix Y-axis label duplication and visibility of workout frequency stats.

## Scope
1.  **Duplicate Y-Axis Labels:**
    -   Update `HealthioChart` to use `AxisItemPlacer.Vertical.step(1f)` when max value is low (e.g. <= 5) to prevent rounded floating-point labels from repeating (e.g. 0, 1, 1, 2, 2).
2.  **Missing Stats Visibility:**
    -   Update `SummaryDetail` in `StatsScreen.kt` to explicitly set `color = MaterialTheme.colorScheme.onSecondaryContainer` for the value text.
    -   Increase contrast for labels.
3.  **Frequency Section UI:**
    -   Adjust spacing in the frequency row to ensure values are visible and not clipped.

## Success Criteria
-   Y-axis shows "0, 1, 2" instead of "0, 1, 1, 2, 2".
-   "This Week/Month/Year" values are clearly visible in the workout stats.
