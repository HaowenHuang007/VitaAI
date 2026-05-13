package com.vitaai.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vitaai.app.MainActivity
import com.vitaai.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val CHANNEL_ID = "vitaai_reminders"
    const val WATER_WORK = "vitaai_water_reminder"
    const val EVENING_WORK = "vitaai_evening_reminder"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "VitaAI",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Recordatorios de salud"
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    fun scheduleWaterReminder(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(WATER_WORK); return }
        val req = PeriodicWorkRequestBuilder<WaterReminderWorker>(3, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().build())
            .build()
        wm.enqueueUniquePeriodicWork(WATER_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun scheduleEveningReminder(context: Context, enabled: Boolean) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) { wm.cancelUniqueWork(EVENING_WORK); return }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            if (timeInMillis < System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelay = cal.timeInMillis - System.currentTimeMillis()
        val req = PeriodicWorkRequestBuilder<EveningReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(EVENING_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun loadPrefs(context: Context): Pair<Boolean, Boolean> {
        val p = context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
        return p.getBoolean("notif_water", false) to p.getBoolean("notif_evening", false)
    }

    fun savePref(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences("vitaai_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    internal fun show(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, notification)
    }
}

class WaterReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationScheduler.show(
            applicationContext,
            1001,
            LanguageManager.t("notif_water_title"),
            LanguageManager.t("notif_water_body")
        )
        return Result.success()
    }
}

class EveningReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationScheduler.show(
            applicationContext,
            1002,
            LanguageManager.t("notif_evening_title"),
            LanguageManager.t("notif_evening_body")
        )
        return Result.success()
    }
}
