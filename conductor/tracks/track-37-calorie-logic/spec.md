# Specification - Track 37: Calorie Burn Logic

## Goal
Refine the daily energy expenditure (Calories Burned) calculation to prioritize Health Connect data when available, while ensuring manual workouts are always additive and fallback logic uses the user's BMR settings.

## Logic Rules
1.  **Health Connect Sync Available:**
    *   **Baseline:** Use the "Total Calories" (Active + Resting) from Health Connect for the day.
    *   **Manual Override:** If the user has logged manual workouts *in Healthio*, add these calories *on top* of the Health Connect Total.
    *   **Individual Imports:** Do NOT double-count individual workouts imported from Health Connect if the "Total Calories" is already being used.
2.  **No Health Connect Sync (Fallback):**
    *   **Baseline:** Use the user's `Base Daily Burn` (BMR) from Settings.
    *   **Additions:** Add all Manual Workouts logged in Healthio.
    *   **Estimation:** Distribute the BMR throughout the day (pro-rated) if viewing "Today", or use the full BMR for past days.

## Implementation Details
*   **Data Model:** Introduce a specific `WorkoutLog` type (e.g., `"Health Connect Daily"`) to store the synced total.
*   **ViewModel:** Update `WorkoutViewModel` to sync this "Daily Total" record.
*   **Stats:** Update `StatsViewModel` to calculate the daily sum based on the rules above (Check for HC Daily record ? Use it + Manual : Use BMR + Manual).

## User Feedback
"if the user synched with health connect than override the burned calories. If there was no manual workout entry, you can override all the calories burned. If there is a manual entry than you should keep it and add it to the value you bring from health connect. If there is no sync with health connect, then the default behavior would be to take the daily energy that the user set in the settings and estimate energy consumption during the day"
