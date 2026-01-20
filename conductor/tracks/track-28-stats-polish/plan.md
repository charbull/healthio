# Implementation Plan - Track 28: Chart & Stats Polish

- [ ] Modify `app/src/main/java/com/healthio/ui/stats/HealthioChart.kt`:
    - [ ] Calculate `maxValue` of the series.
    - [ ] If `maxValue <= 5`, use `AxisItemPlacer.Vertical.step(1f)`.
- [ ] Modify `app/src/main/java/com/healthio/ui/stats/StatsScreen.kt`:
    - [ ] Update `SummaryDetail` to use explicit colors for both label and value.
    - [ ] Use `onSecondaryContainer` for better contrast within the summary card.
- [ ] Verify build and tests.
