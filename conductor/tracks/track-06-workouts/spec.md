# Track 06: Workout Tracking & Health Connect

**Goal:** Implement workout logging via manual entry and Health Connect sync.

## Scope
1.  **Data Layer:**
    -   `WorkoutLog` entity (type, calories, duration, source, timestamp).
    -   `WorkoutDao` with daily aggregation.
2.  **Health Connect:**
    -   Set up permissions for Exercise sessions.
    -   Implement "Fetch latest" logic to import from Garmin/Samsung Health.
3.  **UI:**
    -   "Log Workout" button on Home Screen.
    -   Selection Dialog: Health Connect vs Manual.
    -   Manual Entry Form.
4.  **Dashboard:**
    -   Update Calorie Summary: `Intake [Food] - Burned [Workouts] = Net`.

## Success Criteria
-   User can manually log a workout.
-   User can import a workout from Health Connect.
-   Burned calories are subtracted from today's total on the Home Screen.
