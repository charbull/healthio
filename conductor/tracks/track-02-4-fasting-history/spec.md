# Track 02.4: Fasting History & Charts

**Goal:** Persist completed fasts and visualize weekly progress using a Bar Chart.

## Scope
1.  **Dependencies:** Add `Room` (Database) and `Vico` (Charts) to `build.gradle.kts`.
2.  **Data Layer:**
    -   Entity: `FastingLog` (id, startTime, endTime, durationMillis).
    -   DAO: `FastingDao` (insert, getLast7Days).
    -   Database: `AppDatabase`.
    -   Repository: Update `FastingRepository` to use DAO.
3.  **UI Layer:**
    -   **Chart:** Create `WeeklyFastingChart` using Vico.
    -   **Screen:** Create `StatsScreen`.
    -   **Navigation:** Add `NavHost` to `MainActivity` (Home <-> Stats).
4.  **Logic:**
    -   On `confirmEndFast()`: Save log to DB -> Navigate to Stats.

## Success Criteria
-   Completed fasts are saved to SQLite (Room).
-   "End Fast" flow transitions to a Stats screen.
-   Stats screen displays a Bar Chart of the current week's fasting hours.
