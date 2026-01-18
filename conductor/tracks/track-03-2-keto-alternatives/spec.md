# Track 03.2: Keto Alternatives Logic

**Goal:** Automatically suggest keto-friendly alternatives if the analyzed dish is high in carbs.

## Scope
1.  **Prompt Engineering:**
    -   Instruct Gemini to evaluate Keto suitability.
    -   If not keto-friendly, provide alternatives.
2.  **Logic:**
    -   Include this instruction in the system prompt.

## Success Criteria
-   Scanning a pizza -> Feedback includes "Not Keto. Try a cauliflower crust or zucchini boat instead."
-   Scanning a steak -> Feedback is "Excellent Keto meal."
