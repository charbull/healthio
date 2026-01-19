# Implementation Plan - Track 19: Meal Entry Enhancement

## Phase 1: AI Prompt & Data
- [ ] Modify `GeminiRepository.kt`:
    - [ ] Update prompt to request `food_name`.
    - [ ] Update `FoodAnalysis` data class to include `food_name`.
- [ ] Modify `VisionViewModel.kt`:
    - [ ] Update to handle `food_name`.
    - [ ] Ensure state allows "Review" mode before saving.

## Phase 2: Review UI (VisionScreen)
- [ ] Modify `VisionScreen.kt`:
    - [ ] Replace immediate result display with a "Review Card".
    - [ ] Show Name, Cals, Macros.
    - [ ] Add "Yep, I'm eating that" (Save) and "Stay away" (Discard) buttons.

## Phase 3: Manual Entry & Navigation
- [ ] Create `AddMealDialog.kt` (Choice: Scan vs Manual).
- [ ] Create `ManualFoodLogDialog.kt` (Inputs: Name, Cals, Macros).
- [ ] Modify `HomeScreen.kt`:
    - [ ] Show `AddMealDialog` when "Meal" is clicked.
    - [ ] Handle "Scan" -> Navigate to Vision.
    - [ ] Handle "Manual" -> Show `ManualFoodLogDialog`.
- [ ] Modify `HomeViewModel.kt`:
    - [ ] Add `logManualMeal(name, calories, macros)` function.
