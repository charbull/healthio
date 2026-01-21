# Track 31: Keto & Insulin Insights

**Goal:** Enhance the AI meal logging to provide "Keto Impact" insights, specifically tracking Net Carbs and an Insulin Index score.

## Scope
1.  **Data Model Update:**
    -   Update `MealLog` to include `fiber` (to calculate Net Carbs) and `insulinScore` (0-100).
    -   Update `FoodAnalysis` to include these fields.
2.  **AI Analysis:**
    -   Update `GeminiRepository` prompt to request:
        -   Estimated Fiber (g).
        -   Estimated Insulin Index Score (0-100).
        -   A brief "Keto Impact" assessment (e.g., "Keto Friendly", "Caution", "High Impact").
3.  **UI Updates:**
    -   **Scan Result (`VisionScreen`):** Display Net Carbs (calculated as Total Carbs - Fiber) and the Insulin Score.
    -   **Visuals:** Add a visual indicator for "Keto Impact".

## Success Criteria
-   Scanning a food item returns estimates for Fiber and Insulin Index.
-   The result screen clearly shows "Net Carbs".
-   The tracked data is persisted in the database.
