package com.sora.medsman.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sora.medsman.data.local.MedsManDatabase
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.repository.MedsManRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DosageAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_DOSAGE) ?: ""
        val unitsPerDose = intent
            .getIntExtra(NotificationHelper.EXTRA_UNITS_PER_DOSE, 1)
            .coerceAtLeast(1)
        val scheduledTime = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
        val originalScheduledTime = intent.getLongExtra(
            NotificationHelper.EXTRA_ORIGINAL_SCHEDULED_TIME,
            scheduledTime
        )

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MedsManDatabase.getDatabase(context)
                val repository = MedsManRepositoryImpl(db)

                when (action) {
                    NotificationHelper.ACTION_TRIGGER_DOSAGE -> {
                        NotificationHelper.showDosageNotification(
                            context = context,
                            medicationId = medicationId,
                            scheduleId = scheduleId,
                            medicationName = medicationName,
                            dosage = dosage,
                            unitsPerDose = unitsPerDose,
                            scheduledTime = originalScheduledTime
                        )
                    }

                    NotificationHelper.ACTION_TAKE_MEDICATION -> {
                        NotificationHelper.cancelNotification(context, scheduleId.toInt())
                        val inventoryBefore =
                            db.inventoryDao().getInventoryForMedicationSync(medicationId)
                        repository.logDoseAction(
                            medicationId = medicationId,
                            medicationName = medicationName,
                            dosage = dosage,
                            unitsPerDose = unitsPerDose,
                            scheduledTime = originalScheduledTime,
                            status = TakeStatus.TAKEN,
                            notes = "Taken via notification"
                        )

                        // Only alert when this dose actually crosses the refill threshold.
                        val inventory = db.inventoryDao().getInventoryForMedicationSync(medicationId)
                        if (inventory != null &&
                            inventory.refillReminderEnabled &&
                            inventory.currentStock <= inventory.refillThreshold &&
                            (inventoryBefore == null ||
                                inventoryBefore.currentStock > inventoryBefore.refillThreshold)
                        ) {
                            NotificationHelper.showRefillNotification(
                                context = context,
                                medicationId = medicationId,
                                medicationName = medicationName,
                                currentStock = inventory.currentStock
                            )
                        }
                    }

                    NotificationHelper.ACTION_SKIP_MEDICATION -> {
                        NotificationHelper.cancelNotification(context, scheduleId.toInt())
                        repository.logDoseAction(
                            medicationId = medicationId,
                            medicationName = medicationName,
                            dosage = dosage,
                            unitsPerDose = unitsPerDose,
                            scheduledTime = originalScheduledTime,
                            status = TakeStatus.SKIPPED,
                            notes = "Skipped via notification"
                        )
                    }

                    NotificationHelper.ACTION_SNOOZE_MEDICATION -> {
                        NotificationHelper.cancelNotification(context, scheduleId.toInt())
                        NotificationHelper.snoozeDosageAlarm(
                            context = context,
                            medicationId = medicationId,
                            scheduleId = scheduleId,
                            medicationName = medicationName,
                            dosage = dosage,
                            unitsPerDose = unitsPerDose,
                            minutes = 15,
                            originalScheduledTime = originalScheduledTime
                        )
                        repository.logDoseAction(
                            medicationId = medicationId,
                            medicationName = medicationName,
                            dosage = dosage,
                            unitsPerDose = unitsPerDose,
                            scheduledTime = originalScheduledTime,
                            status = TakeStatus.SNOOZED,
                            notes = "Snoozed 15 min"
                        )
                    }
                }

                // A one-shot alarm must be re-armed for the next scheduled occurrence.
                val schedule = db.scheduleDao().getScheduleById(scheduleId)
                if (schedule != null && schedule.isEnabled && schedule.reminderEnabled) {
                    NotificationHelper.scheduleDosageAlarm(
                        context = context,
                        schedule = schedule.toDomain(),
                        medicationName = medicationName,
                        dosage = dosage
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
