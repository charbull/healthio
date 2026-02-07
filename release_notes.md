# Release Notes - v1.1.0

## v1.1.0 - The Cloud & Hardware Update (2026-02-05)

### ☁️ Data Portability
*   **Two-Way Cloud Sync:** Healthio now supports full two-way sync with Google Sheets. Connecting your account will automatically pull your existing history (fasting, meals, and workouts) from your "Healthio Dashboard Data" spreadsheet.
*   **Sync Reliability:** Refactored the backup engine to use batch updates and atomic operations, making syncing faster and preventing data loss or duplicates.

### 🚀 Hardware & Performance
*   **16 KB Page Alignment:** Fully optimized for next-generation hardware. All native libraries are now stored uncompressed and aligned for 16 KB pages, ensuring peak performance on Android 15 (API 35) and future devices.
*   **Signature Security:** Implemented V2 Signing requirements for enhanced device compatibility.

### 🤖 Smart Vision (AI)
*   **Setup Guard:** Added a proactive setup guide for the Gemini API Key. If a key is missing, the app provides clear instructions and a direct link to get one.
*   **Robust Parsing:** Fixed a crash where the AI would return decimal values. Estimates are now gracefully handled and displayed as whole numbers.
*   **Transparency:** Added disclaimers reminding users that AI estimates should be verified with actual labels.

### 📊 Logic & Accuracy
*   **Calorie Precision:** Fixed a major bug where Health Connect data was being double-counted. Individual workouts are now correctly subtracted from the daily active burn.
*   **Refined Energy Dashboard:** The Home Screen keeps live pro-rated BMR, while Progress charts intelligently hide energy data for days without logged activity to prevent large negative balances.
*   **Clean History:** Health Connect sync is now restricted to data from your app installation date onwards.

### ⚙️ Settings & Defaults
*   **Updated Defaults:** Increased the default daily carbs goal to 50g and set the default protein multiplier to 0.8g per kg of body weight.

---

## v1.0.2 - Play Store Compliance & UI Polish (2026-01-31)
...
