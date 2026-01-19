# Track 10: Fix Month Chart UI

**Goal:** Fix the month chart display which is "broken" likely due to horizontal overflow.

## Scope
1.  **Dynamic Chart Styling:**
    -   Update `HealthioChart` to check the number of data points.
    -   If the number of points is high (e.g., > 14), reduce the bar thickness and spacing to ensure all data fits on the screen.
    -   Target: 31 days should fit within standard screen width (~360dp).
    -   Current: 12dp + 12dp = 24dp * 31 = 744dp (Overflow).
    -   New: 6dp + 4dp = 10dp * 31 = 310dp (Fits).

## Success Criteria
-   The Month view chart displays all 30/31 bars within the screen width without clipping or scrolling issues.
