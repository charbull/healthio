# Specification - Track 36: Weight Trend Axis

## Goal
Adjust the Y-axis of the weight trend chart to dynamically scale based on the data range (min and max values) instead of always starting from zero. This provides a more meaningful view of weight fluctuations.

## Requirements
- **Dynamic Range:** The Y-axis should start slightly below the minimum weight in the visible series and end slightly above the maximum.
- **Constraints:** Only apply this to the "Line Chart" (Weight) mode. Other charts (like Macros or Calories) should likely still start from 0 for context.
- **Visuals:** Ensure the "dots" and line remain clearly visible within the new bounds.

## User Feedback
"the weight trend axis, currenlty its at 198 but we are showing from 0 to 198 instead of showing all this, we can maybe show get the diff in weight and show that interval instead. a zero weight does not make sense and human should not be trending towards it"
