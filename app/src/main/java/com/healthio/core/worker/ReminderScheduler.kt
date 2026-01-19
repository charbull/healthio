package com.healthio.core.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun scheduleAll(context: Context) {
        scheduleReminder(context, "BREAKFAST", 10, 0)
        scheduleReminder(context, "LUNCH", 13, 0)
        scheduleReminder(context, "DINNER", 20, 0)
        scheduleReminder(context, "WORKOUT", 21, 0)
    }

    private fun scheduleReminder(context: Context, type: String, hour: Int, minute: Int) {
        val zoneId = ZoneId.systemDefault()
        val now = LocalDateTime.now(zoneId)
        var target = LocalDateTime.of(LocalDate.now(zoneId), LocalTime.of(hour, minute))

        if (now.isAfter(target)) {
            target = target.plusDays(1)
        }

        val delay = Duration.between(now, target).toMillis()

        val data = Data.Builder().putString("TYPE", type).build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        // Use REPLACE so we don't stack up duplicates if called multiple times, but APPEND could work too.
        // REPLACE ensures we always target the *next* occurrence accurately.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "Reminder_$type",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
