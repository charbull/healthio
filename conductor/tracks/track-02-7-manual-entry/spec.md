# Track 02.7: Manual Fast Entry

**Goal:** Allow users to manually log a completed past fast by selecting both Start and End times.

## Scope
1.  **ViewModel:** Add `logPastFast(startTime, endTime)` to `HomeViewModel`.
    -   Calculates duration.
    -   Saves directly to DB (History).
    -   Does NOT change the current active timer state (unless desired? No, usually manual log is for history).
2.  **UI (HomeScreen):**
    -   Update the "Start from specific time..." flow.
    -   Renaming to "Log Past Fast" or similar?
    -   Actually, the user might want to *start* a fast that is still ongoing (Start Time only) OR log a *completed* fast (Start + End).
    -   **Decision:** Add a separate button "Log Past Fast" or add a "End Now?" checkbox in the dialog?
    -   **Refined UX:**
        1.  Click "Log Manual Fast".
        2.  Pick Start Date/Time.
        3.  Dialog asks: "Is this fast ongoing?" (Yes/No).
        4.  If No (Completed) -> Pick End Date/Time -> Save to History.
        5.  If Yes (Ongoing) -> Start Timer from that time.

## Success Criteria
-   User can log a finished fast (yesterday 8pm to today 12pm) without affecting the current timer.
-   User can still start an ongoing fast from the past.
