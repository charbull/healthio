package com.healthio.core.worker

import com.healthio.core.database.FastingLog
import com.healthio.core.database.MealLog
import com.healthio.core.database.WorkoutLog
import com.healthio.core.database.WeightLog

object SpreadsheetParser {

    fun parseFastingRow(row: List<Any?>, timestampIdx: Int): FastingLog? {
        val ts = row.getOrNull(timestampIdx)?.toString()?.toLongOrNull() ?: return null
        val durationHrs = row.getOrNull(3)?.toString()?.toDoubleOrNull() ?: 0.0
        val durationMillis = (durationHrs * 3600000).toLong()
        return FastingLog(
            startTime = ts,
            endTime = ts + durationMillis,
            durationMillis = durationMillis,
            isSynced = true
        )
    }

    fun parseMealRow(row: List<Any?>, timestampIdx: Int): MealLog? {
        val ts = row.getOrNull(timestampIdx)?.toString()?.toLongOrNull() ?: return null
        return MealLog(
            timestamp = ts,
            foodName = row.getOrNull(2)?.toString() ?: "Imported",
            calories = row.getOrNull(3)?.toString()?.toIntOrNull() ?: 0,
            protein = row.getOrNull(4)?.toString()?.toIntOrNull() ?: 0,
            carbs = row.getOrNull(5)?.toString()?.toIntOrNull() ?: 0,
            fat = row.getOrNull(6)?.toString()?.toIntOrNull() ?: 0,
            isSynced = true
        )
    }

    fun parseWorkoutRow(row: List<Any?>, timestampIdx: Int): WorkoutLog? {
        val ts = row.getOrNull(timestampIdx)?.toString()?.toLongOrNull() ?: return null
        return WorkoutLog(
            timestamp = ts,
            type = row.getOrNull(2)?.toString() ?: "Imported",
            calories = row.getOrNull(3)?.toString()?.toIntOrNull() ?: 0,
            durationMinutes = row.getOrNull(4)?.toString()?.toIntOrNull() ?: 0,
            source = "Manual",
            isSynced = true
        )
    }

    fun parseWeightRow(row: List<Any?>, timestampIdx: Int): WeightLog? {
        val ts = row.getOrNull(timestampIdx)?.toString()?.toLongOrNull() ?: return null
        return WeightLog(
            timestamp = ts,
            valueKg = row.getOrNull(2)?.toString()?.toFloatOrNull() ?: 0f,
            source = "Imported",
            isSynced = true
        )
    }
}
