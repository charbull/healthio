# Track 07: Project Refinements & Polish

**Goal:** Address pending tasks from `todo.md` to polish the app's UI and logic.

## Scope
1.  **Typography Refinement:**
    -   Define a custom Typography for the app.
    -   Apply a consistent, modern font (e.g., Montserrat or similar via Google Fonts / Downloaded ttf).
2.  **Dynamic BMR (Calorie Burn):**
    -   Update `HomeViewModel` to calculate "Burned Calories" dynamically throughout the day.
    -   Formula: `Active Burn (Workouts) + (Base Daily Burn * Hours Elapsed Today / 24)`.
3.  **Workout Statistics:**
    -   Ensure "number of exercises per week, month, year" is clearly visible in the Stats screen.
    -   (Optional) Add a "Workout Frequency" summary to the Home Screen or a dedicated card in Stats.
4.  **Sync Flag Verification:**
    -   Double-check `BackupWorker` and `Dao` implementation to ensure `isSynced` is correctly set and used.

## Success Criteria
-   App font is updated and consistent.
-   Today's "Burned" calories on Home Screen increase gradually throughout the day.
-   User can easily see their total exercise sessions for different time ranges.
-   Data synchronization state is correctly tracked in the local database.
