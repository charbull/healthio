# Implementation Plan - Track 36: Weight Trend Axis

- [x] **HealthioChart.kt:**
    - [x] Calculate `minY` from the series data if `isLineChart` is true.
    - [x] Configure `ChartEntryModelProducer` or the Chart's Axis to use a dynamic range.
    - [x] Specifically, use `Auto` value formatting or explicitly set the axis `min` value if Vico supports it via `AxisValueFormatter` or `AxisItemPlacer` context, but Vico's `Chart` usually handles this if `startAxis` isn't forced to 0.
    - [x] Check if `min` is currently defaulted to 0 in Vico and override it.
- [x] **Verification:**
    - [x] Verify that weight chart Y-axis starts near the lowest weight value (e.g., if weights are 190-200, axis should be ~185-205, not 0-205).
    - [x] Verify that other charts (Bar charts) still start at 0.
