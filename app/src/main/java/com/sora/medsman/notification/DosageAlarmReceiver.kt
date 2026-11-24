package com.sora.medsman.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sora.medsman.R
import com.sora.medsman.data.local.MedsManDatabase
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.repository.MedsManRepositoryImpl
import com.sora.medsman.util.HistoryNotes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DosageAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1L)
        val medicationName = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME)
            ?: context.getString(R.string.notification_default_medication_name)
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
                            notes = HistoryNotes.TAKEN_VIA_NOTIFICATION
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
                            notes = HistoryNotes.SKIPPED_VIA_NOTIFICATION
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
                            notes = HistoryNotes.SNOOZED_15_MIN
                        )
                    }
                }
            } catch (e: Exception) {
                // A failed notification or log must never prevent the next dose
                // from being scheduled.
                e.printStackTrace()
            } finally {
                try {
                    rearmNextDose(
                        context = context,
                        scheduleId = scheduleId,
                        fallbackName = medicationName,
                        fallbackDosage = dosage
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    /**
     * One-shot alarms must be re-armed for the next occurrence. Current
     * medication values are used so renamed medications or changed doses show
     * correctly in the next notification.
     */
    private suspend fun rearmNextDose(
        context: Context,
        scheduleId: Long,
        fallbackName: String,
        fallbackDosage: String
    ) {
        if (scheduleId <= 0L) return

        val db = MedsManDatabase.getDatabase(context)
        val schedule = db.scheduleDao().getScheduleById(scheduleId) ?: return
        if (!schedule.isEnabled || !schedule.reminderEnabled) return

        val medication = db.medicationDao().getMedicationByIdSync(schedule.medicationId)
        NotificationHelper.scheduleDosageAlarm(
            context = context,
            schedule = schedule.toDomain().let { domain ->
                if (medication != null) {
                    domain.copy(dosageQuantity = medication.unitsPerDose.toString())
                } else {
                    domain
                }
            },
            medicationName = medication?.name ?: fallbackName,
            dosage = medication?.dosage ?: fallbackDosage
        )
    }
}
