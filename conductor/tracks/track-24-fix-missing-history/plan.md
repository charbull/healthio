# Implementation Plan - Track 24: Fix Missing History Data

- [ ] **Test Refinement:**
    - [ ] Add a test case to `StatsAggregationTest` that uses `System.currentTimeMillis()` (mocked or actual) to verify `StatsUtils` behavior for "NOW".
- [ ] **Code Verification:**
    - [ ] Review `AddWorkoutDialog` timestamp logic.
    - [ ] Review `StatsViewModel` flow collection.
- [ ] **Potential Fixes:**
    - [ ] If `StatsUtils` is strict on time boundaries (e.g. `isBefore` vs `!isAfter`), relax or verify.
    - [ ] If `StatsScreen` isn't recomposing, check state hoisting.
