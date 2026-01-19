# Track 12: Stats UI Overhaul

**Goal:** Refactor the Stats Screen to display a comprehensive vertical dashboard instead of tabbed views.

## Scope
1.  **Layout Structure:**
    -   Remove `StatType` tabs.
    -   Keep `TimeRange` selector (Week/Month/Year) at the top.
    -   Create a scrollable vertical view containing 3 distinct sections:
        1.  **Fasting:** Chart (Hours) + Summary (Consistency).
        2.  **Energy & Workout:** 
            -   Chart comparing Calorie Intake vs. Burned (Grouped Bar or Multi-Line).
            -   Workout Frequency Summary (Sessions).
        3.  **Food Intake (Nutrition):**
            -   Chart showing Macros (Protein/Carbs/Fat).
            -   Summary.

2.  **ViewModel Refactor:**
    -   Update `StatsViewModel` to calculate and expose data for *all three* sections simultaneously based on the selected `TimeRange`.
    -   Expose `fastingChartData`, `energyChartData`, `nutritionChartData` flows.

## Success Criteria
-   User sees Fasting, Energy/Workout, and Nutrition stats in one continuous scrollable view.
-   Changing the Time Range updates all sections immediately.
