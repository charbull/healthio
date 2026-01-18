# Track 03: Smart Vision (AI) - BYOK

**Goal:** Implement AI-powered food scanning where the user supplies their own Gemini API Key.

## Scope
1.  **Settings:**
    -   Add "Gemini API Key" input field.
    -   Add instructions and a link to Google AI Studio.
    -   Save key to DataStore.
2.  **Camera:**
    -   Use `CameraX` for preview and image capture.
    -   Implement `CameraPermission` handling.
3.  **AI Integration:**
    -   Use `google_generative_ai` SDK.
    -   Construct prompt: "Analyze this image... return JSON...".
    -   Handle "Missing Key" error (prompt user to settings).
4.  **Result Screen:**
    -   Show analyzed data (Calories, Protein, Carbs, Fat).
    -   Allow editing.
    -   Save to History (DB).

## Success Criteria
-   User can click a link to get a key.
-   User can save the key in the app.
-   User can take a photo of food.
-   App displays accurate nutrition info.
