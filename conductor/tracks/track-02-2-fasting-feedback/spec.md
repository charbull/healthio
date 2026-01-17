# Track 02.2: Fasting Feedback

**Goal:** Provide positive reinforcement and educational content upon completing a fast.

## Scope
1.  **Data:** List of motivational/educational quotes about Intermittent Fasting.
2.  **UI:** "Fast Completed" Dialog or Overlay.
    -   "Well Done!" Header.
    -   Total time fasted.
    -   Random Quote.
3.  **Logic:**
    -   Intercept `endFast()` action.
    -   Calculate final duration.
    -   Show feedback before resetting to "Ready" state.

## Success Criteria
-   Clicking "End Fast" shows a summary dialog.
-   Dialog displays correct duration and a random quote.
-   Dismissing dialog resets timer to 0.
