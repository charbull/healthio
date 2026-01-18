# Implementation Plan - Track 03

## Phase 1: Dependencies & Settings
- [x] Add `camera-camera2`, `camera-lifecycle`, `camera-view`.
- [x] Add `google-ai-client` (Generative AI SDK).
- [x] Update `SettingsViewModel` to manage `gemini_api_key`.
- [x] Update `SettingsScreen` with Input Field + Link.

## Phase 2: Camera Feature
- [x] Create `CameraScreen` composable (Integrated into VisionScreen).
- [x] Implement `CameraPreview` view.
- [x] Add Capture logic.

## Phase 3: AI Service
- [x] Create `GeminiRepository`.
- [x] Implement `analyzeImage(bitmap, apiKey)`.
- [x] Define `FoodAnalysis` data model.

## Phase 4: Integration
- [x] Create `AnalysisResultScreen` (ResultContent).
- [x] Wire up: Home -> Camera -> Analyze -> Result.

## Phase 5: Validation
- [x] Test with valid key -> Success.
- [x] Test with invalid/missing key -> Error/Prompt.
