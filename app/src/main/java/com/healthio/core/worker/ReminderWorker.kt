package com.healthio.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthio.MainActivity
import com.healthio.R
import com.healthio.core.database.AppDatabase
import java.time.LocalDate
import java.time.ZoneId

class ReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(context)
    private val mealDao = db.mealDao()
    private val workoutDao = db.workoutDao()

    override suspend fun doWork(): Result {
        val type = inputData.getString("TYPE") ?: return Result.failure()
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val todayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // Logic based on type
        var shouldNotify = false
        var title = ""
        var message = ""

        when (type) {
            "BREAKFAST" -> {
                // Check 4 AM to 10 AM
                val end = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
                val count = mealDao.getCountBetween(todayStart, end) // assuming todayStart is 00:00, effectively checking 00-10
                if (count == 0) {
                    shouldNotify = true
                    title = "Breakfast Reminder"
                    message = "Have you had breakfast yet? Log it now!"
                }
            }
            "LUNCH" -> {
                // Check 10 AM to 1 PM
                val start = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
                val end = today.atTime(13, 0).atZone(zoneId).toInstant().toEpochMilli()
                val count = mealDao.getCountBetween(start, end)
                if (count == 0) {
                    shouldNotify = true
                    title = "Lunch Reminder"
                    message = "Time for lunch? Don't forget to log it!"
                }
            }
            "DINNER" -> {
                // Check 1 PM to 8 PM
                val start = today.atTime(13, 0).atZone(zoneId).toInstant().toEpochMilli()
                val end = today.atTime(20, 0).atZone(zoneId).toInstant().toEpochMilli()
                val count = mealDao.getCountBetween(start, end)
                if (count == 0) {
                    shouldNotify = true
                    title = "Dinner Reminder"
                    message = "Dinner time! Log your meal."
                }
            }
            "WORKOUT" -> {
                // Check all day until 9 PM
                val end = today.atTime(21, 0).atZone(zoneId).toInstant().toEpochMilli()
                val count = workoutDao.getCountBetween(todayStart, end)
                if (count == 0) {
                    shouldNotify = true
                    title = "Workout Reminder"
                    message = "Did you move today? Log your workout!"
                }
            }
        }

        if (shouldNotify) {
            showNotification(title, message)
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "smart_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Smart Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this exists or use system icon
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
