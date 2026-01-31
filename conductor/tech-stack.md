# Tech Stack

## Mobile (Android Native)
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture principles
- **Dependency Injection:** Hilt (or manual dependency injection for simplicity if preferred, but Hilt is standard)
- **Asynchrony:** Coroutines & Flow

## Libraries
- **Charts:** Vico (Modern Compose-first charting) or MPAndroidChart. (Will use Vico for "Minimalist" vibe).
- **Health Data:** Android Health Connect API (via Jetpack Health Connect).
- **Network:** Retrofit + OkHttp (for Gemini API) or Google AI Client SDK for Android.
- **Image Loading:** Coil.
- **Local Database:** Room (if needed beyond Firestore) or DataStore for simple prefs.

## Backend / Services
- **Database:** Firebase Firestore
- **AI/ML:** Gemini 1.5 Flash API (via Google AI Client SDK or REST).
- **Auth:** Firebase Auth.

## Key Constraints
- **Image Processing:** Client-side compression (max 1024px, 80% quality) using standard Android Bitmap APIs.
- **Privacy:** Weight data read-only from Health Connect.
