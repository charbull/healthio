# Track 32: Health Connect Sync

**Goal:** Enable users to pull workout data (burned calories) from Health Connect (e.g., Garmin, Samsung Health) instead of relying solely on manual entry.

## Scope
1.  **Logic:**
    -   Wire up `HealthConnectManager` in `WorkoutViewModel`.
    -   Implement `syncWorkouts()` to fetch `ExerciseSessionRecord` from Health Connect for the current day.
    -   Convert these records into `WorkoutLog` entries.
    -   Prevent duplicates (check if `externalId` already exists).
2.  **UI:**
    -   Add a "Sync from Health Connect" button to the `AddWorkoutDialog`.
    -   Handle permissions request if not granted.
    -   Show success/error toast.

## Success Criteria
-   Clicking "Sync" requests Health Connect permissions (if needed).
-   Workouts from the current day (e.g., a Garmin run) appear in the app's workout log.
-   Calories active burn is updated on the Home Screen.
