# Implementation Plan - Track 34: Weight History Refinement

- [x] **StatsViewModel:**
    - [x] Update `updateChartData` to generate rolling 7-day labels when in `TimeRange.Week`.
    - [x] Ensure robust label formatting.
- [x] **WeightSeriesCalculator:**
    - [x] Update `calculate` to use a 7-day rolling window if requested (or adjust the start date logic for `TimeRange.Week`).
- [x] **HealthioChart:**
    - [x] Fix `spacing = 0` to show all weekly labels.
    - [x] Fix `lineColor` bug (using `toArgb()` instead of `hashCode()`).
    - [x] Add robustness to axis value formatters.
- [x] **Verification:**
    - [x] Verify that the weight trend shows 7 points (today and previous 6 days).
    - [x] Verify labels match the actual days (e.g., if today is Monday, labels should be Tue, Wed, Thu, Fri, Sat, Sun, Mon).
    - [x] Fixed crash when navigating to stats.
