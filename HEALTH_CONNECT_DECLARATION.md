# Google Play Console: Health Connect Declaration

### 1. Which health data types does your app access?
- Workout
- Active Calories Burned
- Weight
- Basal Metabolic Rate (BMR)

### 2. What is the core purpose of your app that requires this data?
Healthio is a minimalist health and fasting dashboard. The core purpose is to provide users with a unified view of their energy balance (Calorie Intake vs. Burned) and fasting consistency. 

### 3. Detailed Description of Data Usage:
- **Workouts & Active Calories:** We import these to calculate the "Energy Burned" side of the dashboard, allowing users to see their net calorie balance for the day.
- **Weight:** We visualize weight trends over time using charts to help users track the long-term impact of their fasting and nutrition habits.
- **BMR:** We use this to estimate baseline energy expenditure when active tracking is not available, ensuring a more accurate energy dashboard.

### 4. Privacy & Security:
All health data is stored locally on the user's device and optionally synced only to the user's own Google Drive (via Google Sheets). Healthio does not transfer health data to any third-party servers.
