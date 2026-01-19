# Track 19: Meal Entry Enhancement

**Goal:** Enhance the "Add Meal" experience to allow both AI-powered scanning (with review) and manual text entry.

## Scope
1.  **Entry Point (Home Screen):**
    -   When "Add Meal" is clicked, show a choice dialog: "Scan Food (AI)" or "Manual Entry".
2.  **Manual Entry:**
    -   A simple input dialog or screen.
    -   Fields: Food Name (Required), Calories (Required), Protein/Carbs/Fat (Optional).
    -   Action: "Log Meal".
3.  **AI Scan Flow (Update):**
    -   **Prompt Engineering:** Update Gemini prompt to explicitly ask for a short, descriptive `food_name` in the JSON response.
    -   **Review UI:** After analysis, show a "Review" card overlay instead of auto-logging (if it was auto-logging) or editable fields.
        -   Display: Generated Name, Calories, Macros, Health Score/Feedback.
        -   Actions:
            -   **"Yep, I'm eating that"**: Confirms and saves to DB.
            -   **"Stay away"**: Discards the result and returns to camera/home.

## Success Criteria
-   User can manually log a meal without camera.
-   User can scan food, see the AI's prediction (Name + Stats), and choose to eat (log) or discard it.
