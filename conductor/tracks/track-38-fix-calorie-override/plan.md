# Implementation Plan - Calorie Override Logic

## Phase 1: Data Access & Research
- [x] Task: Research existing Health Connect implementation in `HealthConnectManager` or similar repository.
- [x] Task: Verify if Health Connect allows querying active calories for specific time intervals (to isolate workout windows).
- [x] Task: Ensure Manual Workouts are being stored with precise `startTime` and `endTime`.

## Phase 2: Core Logic Implementation
- [x] Task: Create or update a `CalorieCalculator` Use Case to handle the aggregation.
- [x] Task: Implement logic:
    -   Fetch total Health Connect active calories for the day.
    -   Identify time windows for manual Healthio workouts.
    -   Fetch Health Connect calories specifically *within* those windows.
    -   Calculate Result: `(Daily HC Total) - (HC Calories during Manual Workouts) + (Manual Workout Calories)`.
- [x] Task: Add Unit Tests for the `CalorieCalculator` logic covering various overlap scenarios.

## Phase 3: Home Screen Integration
- [x] Task: Update `HomeViewModel` to use the new `CalorieCalculator`.
- [x] Task: Ensure the calorie display on the Home Screen triggers a refresh when a manual workout is added or deleted.

## Phase 4: Validation
- [x] Task: Verify: Home Screen displays HC data when no manual workouts exist.
- [x] Task: Verify: Home Screen updates immediately when a 500kcal manual workout is added, overriding any HC data in that window.
- [x] Task: Verify: Deleting a manual workout reverts the Home Screen to showing full Health Connect data.
