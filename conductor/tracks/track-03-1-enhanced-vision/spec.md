# Track 03.1: Enhanced Smart Vision Prompting

**Goal:** Allow users to add optional context (e.g., recipe, ingredients) to the food scan to improve AI accuracy.

## Scope
1.  **UI Flow:**
    -   Current: Snap -> Immediate Analyze.
    -   New: Snap -> **Review Screen** (Show Image + Add Context) -> Analyze.
2.  **Input:**
    -   Add a text field "Add context (optional)".
3.  **Prompt Engineering:**
    -   Append user context to the Gemini prompt.

## Success Criteria
-   User can type "This is a vegan burger made with lentils" before analyzing.
-   AI prompt includes this text.
-   Result reflects the context.
