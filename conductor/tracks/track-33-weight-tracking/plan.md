# Implementation Plan - Track 33: Weight Tracking

- [x] **Data Layer:**
    - [x] Create `WeightLog.kt`.
    - [x] Create `WeightDao.kt`.
    - [x] Update `AppDatabase.kt` (Version 7).
    - [x] Create `WeightRepository.kt`.
- [x] **Health Connect:**
    - [x] Update `AndroidManifest.xml` with `READ_WEIGHT`.
    - [x] Update `HealthConnectManager.kt` to fetch `WeightRecord`.
- [x] **Logic:**
    - [x] Update `WorkoutViewModel` to handle weight sync.
- [x] **UI:**
    - [x] Add a Weight display to `HomeScreen`.
    - [x] Add "Sync Weight" capability (via existing sync button).