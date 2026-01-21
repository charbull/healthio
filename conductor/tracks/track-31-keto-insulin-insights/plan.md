# Implementation Plan - Track 31: Keto & Insulin Insights

- [x] **Data Layer:**
    - [x] Modify `FoodAnalysis` data class in `com.healthio.core.ai`.
    - [x] Modify `MealLog` entity in `com.healthio.core.database`.
    - [x] Increment Room Database version in `AppDatabase.kt`.
- [x] **AI Layer:**
    - [x] Update prompt in `GeminiRepository.kt` to request Fiber and Insulin Index.
    - [x] Ensure JSON parsing handles new fields.
- [x] **UI Layer:**
    - [x] Update `VisionScreen.kt` (`ResultContent`) to display:
        -   Fiber.
        -   Net Carbs (Carbs - Fiber).
        -   Insulin Index Score.
    - [x] Add a "Keto Impact" section or badge.
    - [x] Update `VisionViewModel.kt` to map `FoodAnalysis` to the new `MealLog` fields during save.