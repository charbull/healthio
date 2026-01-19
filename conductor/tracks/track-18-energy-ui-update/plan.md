# Implementation Plan - Track 18: Energy UI Update

- [ ] Modify `app/src/main/java/com/healthio/ui/dashboard/HomeScreen.kt`:
    - [ ] **Scaffold:** Remove `floatingActionButton` parameter.
    - [ ] **EnergySection:**
        - [ ] Update `onAddWorkout` to be passed down.
        - [ ] Add `onAddMeal` callback parameter.
        - [ ] Inside Card:
            - [ ] Remove header row "+" `IconButton`.
            - [ ] Below the Stats Row, add `Spacer` and a `Row` of buttons.
            - [ ] Button 1: "Add Meal" (calls `onAddMeal`).
            - [ ] Button 2: "Add Workout" (calls `onAddWorkout`).
    - [ ] **HomeScreen:** Pass `onNavigateToVision` to `EnergySection`'s `onAddMeal`.
