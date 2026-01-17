# Track 02.1: Flux Timer Refinement

**Goal:** Refine the Flux Timer to support persistent state, manual start times, and accurate progress calculation.

## Scope
1.  **Data Persistence:** Use `DataStore` (Preferences) to save:
    -   `isFasting` (Boolean)
    -   `fastStartTime` (Long, Timestamp)
    -   `targetDuration` (Long, Default 16 hours)
2.  **Logic Updates (HomeViewModel):**
    -   On `init`: Load state from DataStore.
    -   `startFast(startTime: Long)`: Save start time and state.
    -   `endFast()`: Clear state, log completed fast (eventually to Firestore, just local clear for now).
    -   `calculateProgress()`: Dynamic update based on `CurrentTime - StartTime`.
3.  **UI Updates (HomeScreen):**
    -   **Active State:** Show Progress Ring + "End Fast" button.
    -   **Inactive State:** Show "Start Fasting" (Primary) + "Set Start Time" (TextButton/Secondary).
    -   **Time Picker:** Simple dialog to pick a past time if "Set Start Time" is clicked.

## Success Criteria
-   App remembers fasting state after restart.
-   User can start a fast from "Now" or a "Past Time".
-   Progress ring accurately reflects time elapsed since `fastStartTime`.
