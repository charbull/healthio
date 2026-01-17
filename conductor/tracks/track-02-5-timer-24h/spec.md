# Track 02.5: Flux Timer 24h Visuals

**Goal:** Visualise fasting duration as 24-hour cycles using concentric rings.

## Scope
1.  **HomeViewModel:** Update progress calculation.
    -   Pass `elapsedMillis` directly to UI instead of normalized 0-1 float.
    -   Or pass `days` and `currentDayProgress`.
2.  **FluxTimer:** Rewrite drawing logic.
    -   **Background:** 24h track.
    -   **Ring 1 (Outer):** Represents Day 1.
    -   **Ring 2 (Inner):** Represents Day 2 (if applicable).
    -   **Ring N:** Continues inwards for longer fasts.

## Success Criteria
-   Fasting < 24h shows a single partial ring (e.g., 12h = 50%).
-   Fasting 26h shows 1 Full Outer Ring + 1 Partial Inner Ring (2h worth).
-   Visuals remain clean (high contrast).
