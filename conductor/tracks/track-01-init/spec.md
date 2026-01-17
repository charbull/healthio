# Track 01: Project Initialization (Android Native)

**Goal:** Initialize a Native Android project (Kotlin/Compose), set up build scripts, and establish the core architecture.

## Scope
1.  **Project Scaffold:** Create the folder structure and Gradle build files for a multi-module or standard single-module Android app.
    -   `app/` module.
    -   `gradle/` wrapper setup.
2.  **Dependencies:** Configure `build.gradle.kts` with:
    -   Jetpack Compose (Material 3).
    -   Firebase BOM.
    -   Health Connect.
    -   Vico (Charts).
    -   Google AI Client SDK (Gemini).
    -   Coil (Images).
    -   Coroutines.
3.  **Architecture:**
    -   `app/src/main/java/com/healthio/` structure.
    -   `ui/theme` (Dark mode default).
4.  **Entry Point:** `MainActivity.kt` rendering a basic "Healthio" screen.

## Success Criteria
-   Project syncs successfully with Gradle.
-   `./gradlew assembleDebug` passes.
-   App installs and runs on an Android Emulator.