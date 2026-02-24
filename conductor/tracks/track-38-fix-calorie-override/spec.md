# Specification: Calorie Override Logic (Manual Workouts vs. Health Connect)

## Overview
Currently, the Healthio app's Home Screen calorie display is being overwritten by data from Health Connect (synced from a watch), even when manual workouts are recorded in Healthio. This track aims to implement a "Manual Priority" logic to ensure Healthio-recorded workouts are the primary source of truth for their specific durations.

## Functional Requirements
1.  **Default Source:** Fall back to Health Connect "Active Calories Burned" for general daily activity tracking.
2.  **Manual Priority:** If a user enters a manual workout in Healthio, the calories from that workout must take precedence over Health Connect data for that specific time window.
3.  **Conflict Resolution (Time-Based):** 
    -   Identify the start and end time of a manual Healthio workout.
    -   Use the Healthio-calculated calories for that duration.
    -   Use Health Connect data for all periods outside of that duration to calculate the total daily "Active Calories Burned."
4.  **UI Integration:** The Home Screen (Main Dashboard) should reflect this aggregated total.

## Non-Functional Requirements
-   **Accuracy:** Ensure no "double counting" occurs by properly isolating Health Connect data from the manual workout windows.
-   **Performance:** The calculation should be efficient enough to run during the standard sync/refresh cycle on the Home Screen.

## Acceptance Criteria
-   [ ] When no manual workout exists, Home Screen shows active calories from Health Connect.
-   [ ] When a manual workout is added, the total daily calories burned is calculated as: `(HC Calories outside workout window) + (Healthio Manual Workout Calories)`.
-   [ ] The Home Screen updates correctly after adding or deleting a manual workout.

## Out of Scope
-   Modifying how Health Connect data is written (this is a read-side logic change).
-   Changes to the "Smart Vision" calorie intake logic.
