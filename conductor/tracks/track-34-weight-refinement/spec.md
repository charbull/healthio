# Specification - Track 34: Weight History Refinement

## Goal
Improve the weight history visualization to show the last 7 days (including today) and ensure the trend is plotted correctly, rather than just the current calendar week.

## Requirements
- Update `WeightSeriesCalculator` to handle "Rolling Week" (last 7 days).
- Update `StatsViewModel` to provide appropriate labels for the last 7 days.
- Ensure carry-forward logic still works for days without logs.
- Plot the trend line clearly.

## User Feedback
"I know the issue with the weight history, you need to go back 7 days before, while the current view is only showing from monday to sunday but you need to go back 6 days back and show the current day and then plot the trend"
