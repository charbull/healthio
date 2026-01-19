# Implementation Plan - Track 08: Fix Fasting Stats

- [ ] Modify `StatsViewModel.kt`:
    - [ ] Inside `updateChartData`, locate `StatType.Fasting` block.
    - [ ] Change `dailyMax` variable name to `dailyTotal` for clarity.
    - [ ] Change aggregation logic from `maxOf(...)` to `+`.
    - [ ] Ensure `dailyTotal` is initialized to 0f.
