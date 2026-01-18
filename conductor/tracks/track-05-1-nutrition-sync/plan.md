# Implementation Plan - Track 05.1

## Phase 1: Data Update
- [x] Add `isSynced` to `MealLog.kt`.
- [x] Update `MealDao.kt` with sync methods.
- [x] Increment `AppDatabase` version to 4.

## Phase 2: Sync Implementation
- [x] Update `BackupWorker.kt`:
    -   Refactor to handle both log types.
    -   Implement `syncMealLogs` logic.
    -   Update tab naming convention (e.g., `Year_Fasting`, `Year_Meals`).

## Phase 3: Validation
- [x] Save a meal -> Trigger worker -> Verify spreadsheet update.
