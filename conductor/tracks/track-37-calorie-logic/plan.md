# Implementation Plan - Track 37: Calorie Burn Logic

- [x] **WorkoutViewModel.kt:**
    - [x] Modify `syncFromHealthConnect`:
        - [x] Fetch `TotalCalories` (not just Active) for the day.
        - [x] Insert/Update a `WorkoutLog` with `type = "Health Connect Daily"`.
        - [x] Stop inserting "Daily Active Burn" adjustment records (the new Daily Total record replaces this).
        - [x] Ensure individual imported workouts (from HC) are still stored for detail views, OR decide to filter them out of the *Sum* calculation if a Daily Total exists. (Better to keep them for the list view, but exclude from Sum in Stats).
- [x] **StatsViewModel.kt:**
    - [x] Update `updateChartData` logic for `burned`:
        - [x] Filter logs for the day.
        - [x] Check if a log with `type == "Health Connect Daily"` exists.
        - [x] **If Exists:** Sum = `HC_Daily.calories` + `Sum(Manual_Workouts)`. (Ignore other HC imported logs to avoid double counting).
        - [x] **If Not Exists:** Sum = `baseDailyBurn` + `Sum(Manual_Workouts)` + `Sum(Other_Workouts)`.
- [x] **Verification:**
    - [x] Verify "No Sync" shows BMR + Manual.
    - [x] Verify "Sync" shows HC Total + Manual.
    - [x] Verify no double counting of BMR or Imported workouts.
