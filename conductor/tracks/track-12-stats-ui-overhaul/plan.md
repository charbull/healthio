# Implementation Plan - Track 12: Stats UI Overhaul

## Phase 1: StatsViewModel Refactor
- [ ] Remove `StatType` enum usage and `_statType` state.
- [ ] Create separate state flows for each section's data:
    - [ ] `fastingState`: { chartData, summaryTitle, summaryValue }
    - [ ] `energyState`: { chartData (Intake vs Burned), workoutSummary }
    - [ ] `nutritionState`: { chartData (Macros), summary }
- [ ] Refactor `updateChartData` to populate all three states for the selected `TimeRange`.

## Phase 2: StatsScreen Refactor
- [ ] Remove `TabRow` for StatTypes.
- [ ] Implement a `Column(Modifier.verticalScroll)` layout.
- [ ] Add "Fasting" Section: Title + Chart + Card.
- [ ] Add "Energy & Workout" Section: Title + Chart (Grouped) + Card.
- [ ] Add "Nutrition" Section: Title + Chart (Stacked/Grouped) + Card.

## Phase 3: Chart Polish
- [ ] Ensure `HealthioChart` supports multiple datasets (already does, but need to check coloring for Energy section).
- [ ] Update colors: Green/Red for Energy/Burned? Or Theme colors.
