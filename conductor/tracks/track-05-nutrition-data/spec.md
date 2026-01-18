# Track 05: Nutrition Data Persistence & Stats

**Goal:** Save analyzed meals to the database and track daily/weekly/monthly macro intake.

## Scope
1.  **Database:**
    -   `MealLog` entity.
    -   `MealDao` with aggregation queries.
2.  **Logic:**
    -   Save meal after analysis.
    -   Calculate daily totals.
3.  **UI:**
    -   **Home:** Show "Today's Intake" (Calories + Macros).
    -   **Stats:** Add "Nutrition" tab (Calories over time).

## Success Criteria
-   "Save" button on Meal Result screen persists data.
-   Home screen updates to show calories consumed today.
