# Implementation Plan - Track 07: Project Refinements & Polish

## Phase 1: Dynamic BMR Calculation
- [ ] Update `HomeViewModel` to calculate the proportional BMR burn based on the current time of day.
- [ ] Ensure the UI updates this value frequently (the existing 1s timer loop can handle this).

## Phase 2: Typography Update
- [ ] Choose a font (e.g., Montserrat).
- [ ] Add font files to `res/font` or use `GoogleFonts` if available/appropriate.
- [ ] Create `Type.kt` and define the `Typography` object.
- [ ] Update `Theme.kt` to use the new `Typography`.
- [ ] Refactor `HomeScreen.kt` header to use the new typography instead of hardcoded Serif/Italic.

## Phase 3: Workout Stats Enhancement
- [ ] Review `StatsViewModel` and `StatsScreen`.
- [ ] Add a more prominent "Exercise Frequency" display if needed.
- [ ] Ensure the "sessions" count is easy to find.

## Phase 4: Sync Flag Check
- [ ] Verify `BackupWorker` logic for all entities (Fasting, Meals, Workouts).
- [ ] Add logging or a simple debug UI if needed to confirm `isSynced` is working.
