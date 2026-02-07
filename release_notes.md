# Release Notes - v1.1.0

## v1.1.0 - The Cloud & Hardware Update (2026-02-05)

### 📊 Logic & Accuracy
*   **Energy Balance Fix:** Resolved a calculation error in the Progress charts where calories were being over-subtracted. The energy balance now accurately reflects your "Active Burn" plus your "Base Burn" without double-counting.
*   **Calorie Precision:** Improved the handling of Health Connect data to ensure that "Total Daily" and "Active Burn" logs are processed correctly across all views.

### ☁️ Data Portability
*   **Two-Way Cloud Sync:** Healthio now supports full two-way sync with Google Sheets. Connecting your account will automatically pull your existing history from your "Healthio Dashboard Data" spreadsheet.
*   **Sync Reliability:** Refactored the backup engine to use batch updates and atomic operations for faster, more reliable syncing.

### 🚀 Hardware & Performance
*   **16 KB Page Alignment:** Fully optimized for next-generation hardware. Native libraries are stored uncompressed and aligned for 16 KB pages for peak performance on Android 15 (API 35).
*   **Signature Security:** Implemented V2 Signing for enhanced device compatibility.

### 🤖 Smart Vision (AI)
*   **Setup Guard:** Added a proactive setup guide for the Gemini API Key.
*   **Robust Parsing:** Fixed decimal parsing for macro estimates.
*   **Transparency:** Added disclaimers for AI-generated estimates.

---

## v1.0.2 - Play Store Compliance & UI Polish (2026-01-31)
...
