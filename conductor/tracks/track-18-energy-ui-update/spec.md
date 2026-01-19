# Track 18: Energy UI Update

**Goal:** Refactor "Today's Energy" card to include "Add Meal" and "Add Workout" buttons, replacing scattered icons.

## Scope
1.  **Remove Old Actions:**
    -   Remove the small "+" icon inside the "Today's Energy" card header (previously for Workouts).
    -   Remove the Floating Action Button (FAB) from the Scaffold (previously for Vision/Food).
2.  **Add New Buttons:**
    -   Inside `EnergySection` card, below the stats row.
    -   Add a `Row` with two buttons:
        -   **Add Meal**: Navigate to Vision/Food flow.
        -   **Add Workout**: Open Workout Dialog.
    -   Style: Side-by-side, equal width (`weight(1f)`), outlined or filled (Tonal) to distinguish from the primary "Fast" button.
3.  **UI Polish:**
    -   Ensure visual hierarchy so the Energy buttons don't compete too much with the main "Start/End Fast" button. Use `OutlinedButton` or `FilledTonalButton`.

## Success Criteria
-   Home Screen has no FAB.
-   Energy Card contains explicit "Add Meal" and "Add Workout" buttons.
-   UI looks balanced.
