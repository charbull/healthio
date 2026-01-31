# Product Definition: Healthio (Android MVP)

**Type:** Personal Health Dashboard
**Vibe:** Minimalist, high-contrast, fast. "Snap & Go" interaction model. Dark mode default.

## Core Features

### 1. "Smart Vision" (AI Nutritionist)
- **Input:** User takes a photo via native Camera.
- **Processing:**
  - Compress image (max 1024px, 80% quality) to save bandwidth.
  - Send to Gemini 2.5 Pro API.
- **Prompt Strategy:** Pass image + user's remaining calorie budget.
- **Output:** JSON with `{items[], total_macros, health_score, feedback_color (HEX), feedback_text}`.
- **UI:** "Analyzing..." overlay -> Editable Meal Card -> Save to Firestore.

### 2. "Flux Timer" (Intermittent Fasting)
- **Visual:** Large circular progress indicator on Home Screen.
- **Logic:** 16:8 Default.
- **State:** Toggle between "FASTING" (Green ring) and "EATING" (Orange ring).
- **Automation:** If a Food Log is created while state is "FASTING", prompt user to end fast.

### 3. "Android Body Sync" (Weight)
- **Logic:** Read-only sync from Android Health Connect.
- **Graph:** 30-day weight trend.
- **Privacy:** Do not store weight on our servers if possible; read live or cache locally.