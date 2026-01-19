# Track 17: Compact Home Screen UI

**Goal:** Adjust the Home Screen layout to fit all content (Header, Fasting, Energy) within the viewport of a standard phone screen without scrolling.

## Scope
1.  **Fasting Section:**
    -   Remove the redundant "FASTING TIME" / "READY" text label (since the Flux Timer visual and the Time Display already convey this).
    -   Reduce vertical spacing (spacers) between elements.
    -   Reduce the size of the Action Button (height 56dp -> 48dp or 50dp).
    -   (Optional) Reduce Flux Timer size slightly if needed.
2.  **Energy Section:**
    -   Reduce internal padding of the "Today's Energy" card.
    -   Reduce spacing between Header and Fasting, and Fasting and Energy.
3.  **Overall Layout:**
    -   Ensure `Arrangement.SpaceBetween` or weighted column is used effectively to distribute space without large fixed gaps.

## Success Criteria
-   The entire Home Screen content is visible on a typical device (e.g., Pixel 5/6 size) without scrolling.
-   The UI still feels "clean" and not cramped.
