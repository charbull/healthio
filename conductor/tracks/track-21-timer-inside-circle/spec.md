# Track 21: Timer Inside Circle

**Goal:** Move the timer text display ("00:00:00") inside the Flux Timer visual ring to save space and unify the visual.

## Scope
1.  **FluxTimer Component Update:**
    -   Add `timeDisplay: String` parameter to `FluxTimer`.
    -   Use a `Box` to overlay the text in the center of the ring.
2.  **HomeScreen Integration:**
    -   Pass `uiState.timeDisplay` into `FluxTimer`.
    -   Remove the external `Text` composable that previously displayed the time below the circle.
3.  **Visual Polish:**
    -   Ensure the text size fits well within the ring (~displayMedium or similar).

## Success Criteria
-   The timer digits are centered inside the circular progress indicator.
-   The vertical space below the circle is reclaimed.
