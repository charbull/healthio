# Track 22: Smart Reminders

**Goal:** Add "Smart Reminders" to prompt users to log meals/workouts at specific times if they haven't already.

## Scope
1.  **Permissions:**
    -   Add `POST_NOTIFICATIONS` to Manifest.
    -   Request permission on App Start (e.g., in `MainActivity` or `HomeScreen`).
2.  **Data Layer Updates:**
    -   Update `MealDao`: Add `getCountBetween(start, end)`.
    -   Update `WorkoutDao`: Add `getCountBetween(start, end)`.
3.  **Worker Logic (`ReminderWorker`):**
    -   Input: `reminderType` (Breakfast, Lunch, Dinner, Workout).
    -   Check DB for logs in the relevant window.
    -   If logs == 0, show Notification.
4.  **Scheduling (`ReminderScheduler`):**
    -   Utility to schedule the 4 daily OneTimeWorkRequests (with initial delay to hit target time).
    -   Reschedule on app start to ensure they are queued for the next window.

## Target Times & Windows
-   **10:00 AM (Breakfast):** Check 04:00 - 10:00.
-   **13:00 PM (Lunch):** Check 10:00 - 13:00.
-   **20:00 PM (Dinner):** Check 13:00 - 20:00.
-   **21:00 PM (Workout):** Check 04:00 - 21:00.

## Success Criteria
-   Notifications appear at (or near) target times if no data exists.
-   Notifications do NOT appear if data exists.
