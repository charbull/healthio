# Healthio

**Healthio** is a minimalist, privacy-focused Android health dashboard designed for intermittent fasting, calorie tracking, and workout logging. It emphasizes a "Snap & Go" interaction model with AI-powered food analysis and seamless integration with Google Sheets for data sovereignty.

<p align="center">
  <img src="demo/demo.gif" width="300" alt="Healthio Demo">
</p>

## Key Features

*   **Flux Timer:** A visual, intuitive timer for Intermittent Fasting (16:8 default) with clear "Fasting" and "Eating" states.
*   **Smart Vision (AI Nutritionist):** Snap a photo of your meal, and Healthio (powered by Gemini AI) analyzes it to estimate calories, macros, and provide a health score with feedback.
*   **Energy Dashboard:** Track your daily Calorie Intake vs. Burned Calories in a simple, unified view.
*   **Smart Reminders:** Intelligent notifications remind you to log meals (Breakfast, Lunch, Dinner) or workouts only if you haven't already.
*   **Privacy-First Sync:** Your data belongs to you. Healthio syncs directly to a Google Sheet in your personal Drive, giving you full control and raw access to your logs.
*   **Workout Logging:** Log workouts manually or sync (future) to keep track of your active energy burn.
*   **Historical Stats:** Visualize your progress with beautiful charts for Fasting consistency, Workout frequency, and Nutrition trends over the Week, Month, and Year.

## Tech Stack

*   **Language:** Kotlin
*   **UI:** Jetpack Compose (Material 3)
*   **Architecture:** MVVM
*   **AI:** Google gemini-2.5-flash-lite (via Generative AI SDK)
*   **Local Data:** Room Database
*   **Sync:** Google Sheets API (v4) & Google Sign-In
*   **Scheduling:** WorkManager
*   **Charts:** Vico

## Getting Started

1.  Clone the repository.
2.  Add your Gemini API Key in the settings (or code configuration).
3.  Build and run on an Android device (Android 13+ recommended for notifications).

## Privacy

Healthio is designed with privacy in mind. It requests the minimum necessary permissions and syncs data to a specific file in your Google Drive, ensuring it cannot access your other personal documents.

---
*Built with ❤️ for a healthier lifestyle.*
