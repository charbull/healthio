# Implementation Plan - Track 06

## Phase 1: Foundation
- [x] Add `androidx.health.connect:connect-client:1.1.0-alpha11` to `build.gradle.kts`.
- [x] Create `WorkoutLog` entity and `WorkoutDao`.
- [x] Increment `AppDatabase` to Version 5.

## Phase 2: Manual Entry UI
- [x] Create `AddWorkoutDialog` with fields for Type, Duration, and Calories.
- [x] Wire up "Manual Entry" flow.

## Phase 3: Health Connect Logic
- [x] Implement `HealthConnectManager` to handle permissions and fetching.
- [x] Implement "Fetch" logic to avoid duplicate imports (using `externalId` or timestamp).

## Phase 4: Dashboard Integration
- [x] Update `HomeViewModel` to fetch `todayBurnedCalories`.
- [x] Update `HomeScreen` to display "Net Calories".

## Phase 5: Backup
- [x] Update `BackupWorker` to sync to `[Year]_Workouts`. (Deferred to Track 06.1 for cleanliness)
