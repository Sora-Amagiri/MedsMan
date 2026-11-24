package com.sora.medsman.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sora.medsman.MainActivity
import com.sora.medsman.R
import com.sora.medsman.data.local.MedsManDatabase
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.util.formatDoseAmount
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_DOSAGE_ID = "meds_dosage_channel"
    const val CHANNEL_REFILL_ID = "meds_refill_channel"

    const val ACTION_TRIGGER_DOSAGE = "com.sora.medsman.ACTION_TRIGGER_DOSAGE"
    const val ACTION_TAKE_MEDICATION = "com.sora.medsman.ACTION_TAKE_MEDICATION"
    const val ACTION_SKIP_MEDICATION = "com.sora.medsman.ACTION_SKIP_MEDICATION"
    const val ACTION_SNOOZE_MEDICATION = "com.sora.medsman.ACTION_SNOOZE_MEDICATION"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_DOSAGE = "extra_dosage"
    const val EXTRA_UNITS_PER_DOSE = "extra_units_per_dose"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_ORIGINAL_SCHEDULED_TIME = "extra_original_scheduled_time"

    fun createNotificationChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val dosageChannel = NotificationChannel(
            CHANNEL_DOSAGE_ID,
            context.getString(R.string.notification_channel_dosage_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_dosage_description)
            enableVibration(true)
        }

        val refillChannel = NotificationChannel(
            CHANNEL_REFILL_ID,
            context.getString(R.string.notification_channel_refill_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_refill_description)
        }

        notificationManager.createNotificationChannel(dosageChannel)
        notificationManager.createNotificationChannel(refillChannel)
    }

    @SuppressLint("MissingPermission")
    fun scheduleDosageAlarm(
        context: Context,
        schedule: Schedule,
        medicationName: String,
        dosage: String,
        skipToday: Boolean = false
    ) {
        if (!schedule.isEnabled || !schedule.reminderEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = calculateNextTriggerTime(
            timeOfDay = schedule.timeOfDay,
            daysOfWeek = schedule.daysOfWeek,
            skipToday = skipToday
        )
        val unitsPerDose = schedule.dosageQuantity.toIntOrNull()?.coerceAtLeast(1) ?: 1

        val intent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_DOSAGE
            putExtra(EXTRA_MEDICATION_ID, schedule.medicationId)
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNITS_PER_DOSE, unitsPerDose)
            putExtra(EXTRA_SCHEDULED_TIME, triggerTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(context, triggerTime, pendingIntent)
    }

    /**
     * Re-arms every enabled schedule. Called on app start, after a reboot or app
     * update, and after system time, timezone, or exact-alarm permission changes.
     */
    suspend fun rescheduleAllReminders(context: Context) {
        val database = MedsManDatabase.getDatabase(context)
        val schedules = database.scheduleDao().getAllSchedules().first()
        val medications = database.medicationDao().getAllMedications().first()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayLogs = database.historyLogDao()
            .getHistoryLogsForDateRange(todayStart, todayEnd - 1L)
            .first()

        schedules.forEach { schedule ->
            if (!schedule.isEnabled || !schedule.reminderEnabled) return@forEach
            val medication = medications.find { it.id == schedule.medicationId }
                ?: return@forEach
            val handledToday = todayLogs.any { log ->
                log.medicationId == schedule.medicationId &&
                    isSameTimeOfDay(log.scheduledTime, schedule.timeOfDay)
            }
            scheduleDosageAlarm(
                context = context,
                schedule = schedule.toDomain().copy(
                    dosageQuantity = medication.unitsPerDose.toString()
                ),
                medicationName = medication.name,
                dosage = medication.dosage,
                skipToday = handledToday
            )
        }
    }

    /**
     * Cancels the reminder for the dose the user just handled in the app and
     * arms the next occurrence, so today's alarm cannot fire after a Taken,
     * Missed, or Skipped action.
     */
    fun cancelTodayDoseAndArmNext(
        context: Context,
        schedule: Schedule,
        medicationName: String,
        dosage: String,
        unitsPerDose: Int,
        scheduledTime: Long
    ) {
        cancelDosageAlarm(context, schedule.id)
        scheduleDosageAlarm(
            context = context,
            schedule = schedule.copy(dosageQuantity = unitsPerDose.toString()),
            medicationName = medicationName,
            dosage = dosage,
            skipToday = isSameLocalDay(scheduledTime, System.currentTimeMillis())
        )
    }

    fun areDosageNotificationsEnabled(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return false
        val channel = notificationManager.getNotificationChannel(CHANNEL_DOSAGE_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        try {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
                .canScheduleExactAlarms()
        } catch (_: Exception) {
            false
        }

    fun snoozeDosageAlarm(
        context: Context,
        medicationId: Long,
        scheduleId: Long,
        medicationName: String,
        dosage: String,
        unitsPerDose: Int = 1,
        minutes: Int = 15,
        originalScheduledTime: Long = System.currentTimeMillis()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_DOSAGE
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNITS_PER_DOSE, unitsPerDose.coerceAtLeast(1))
            putExtra(EXTRA_SCHEDULED_TIME, triggerTime)
            putExtra(EXTRA_ORIGINAL_SCHEDULED_TIME, originalScheduledTime)
        }

        val requestCode = (scheduleId.toInt() * 1000) + 99
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setAlarm(context, triggerTime, pendingIntent)
    }

    fun cancelDosageAlarm(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_DOSAGE
        }
        // Cancel both the daily alarm and any pending 15-minute snooze alarm.
        val requestCodes = listOf(
            scheduleId.toInt(),
            (scheduleId.toInt() * 1000) + 99
        )
        requestCodes.forEach { requestCode ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        // Also remove any reminder notification that is still on screen.
        cancelNotification(context, scheduleId.toInt())
    }

    @SuppressLint("MissingPermission")
    fun showDosageNotification(
        context: Context,
        medicationId: Long,
        scheduleId: Long,
        medicationName: String,
        dosage: String,
        unitsPerDose: Int = 1,
        scheduledTime: Long
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            scheduleId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val takeIntent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_TAKE_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNITS_PER_DOSE, unitsPerDose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_ORIGINAL_SCHEDULED_TIME, scheduledTime)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId.toInt() * 10) + 1,
            takeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_SKIP_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNITS_PER_DOSE, unitsPerDose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_ORIGINAL_SCHEDULED_TIME, scheduledTime)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId.toInt() * 10) + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, DosageAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_MEDICATION
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_MEDICATION_NAME, medicationName)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_UNITS_PER_DOSE, unitsPerDose)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_ORIGINAL_SCHEDULED_TIME, scheduledTime)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (scheduleId.toInt() * 10) + 3,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DOSAGE_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                context.getString(R.string.notification_dosage_title, medicationName)
            )
            .setContentText(
                context.getString(
                    R.string.notification_dosage_text,
                    formatDoseAmount(
                        unitsPerDose,
                        dosage,
                        context.getString(R.string.dose_units_fallback)
                    )
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                context.getString(R.string.notification_action_take),
                takePendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_skip),
                skipPendingIntent
            )
            .addAction(
                0,
                context.getString(R.string.notification_action_snooze),
                snoozePendingIntent
            )
            .build()

        notificationManager.notify(scheduleId.toInt(), notification)
    }

    @SuppressLint("MissingPermission")
    fun showRefillNotification(
        context: Context,
        medicationId: Long,
        medicationName: String,
        currentStock: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            (medicationId.toInt() * 100) + 5,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REFILL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(
                context.getString(R.string.notification_refill_title, medicationName)
            )
            .setContentText(
                context.getString(R.string.notification_refill_text, currentStock)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((medicationId.toInt() * 100) + 5, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }

    private fun setAlarm(
        context: Context,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canUseExactAlarm = try {
            alarmManager.canScheduleExactAlarms()
        } catch (_: Exception) {
            false
        }

        if (canUseExactAlarm) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                return
            } catch (_: Exception) {
                // Permission may be revoked between the check and this call.
            }
        }

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    internal fun calculateNextTriggerTime(
        timeOfDay: String,
        daysOfWeek: List<Int>,
        skipToday: Boolean = false,
        now: Calendar = Calendar.getInstance()
    ): Long {
        val parts = timeOfDay.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Use a small margin so a re-arm performed while the current alarm is
        // firing cannot schedule itself again for the same occurrence. When the
        // dose was already handled today, always start from tomorrow.
        if (skipToday) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        } else if (target.timeInMillis <= now.timeInMillis + 5_000L) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (daysOfWeek.isNotEmpty() && daysOfWeek.size < 7) {
            for (step in 0..7) {
                val calDay = target.get(Calendar.DAY_OF_WEEK)
                val modelDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1
                if (daysOfWeek.contains(modelDay)) {
                    break
                }
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return target.timeInMillis
    }

    private fun isSameTimeOfDay(epochMillis: Long, timeOfDay: String): Boolean {
        val parts = timeOfDay.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return false
        val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
        return calendar.get(Calendar.HOUR_OF_DAY) == hour &&
            calendar.get(Calendar.MINUTE) == minute
    }

    private fun isSameLocalDay(firstMillis: Long, secondMillis: Long): Boolean {
        val first = Calendar.getInstance().apply { timeInMillis = firstMillis }
        val second = Calendar.getInstance().apply { timeInMillis = secondMillis }
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
            first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}
