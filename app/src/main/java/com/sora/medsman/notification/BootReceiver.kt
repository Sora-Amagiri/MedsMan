package com.sora.medsman.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sora.medsman.data.local.MedsManDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MedsManDatabase.getDatabase(context)
                    val schedules = db.scheduleDao().getAllSchedules().first()
                    val meds = db.medicationDao().getAllMedications().first()

                    for (schedule in schedules) {
                        val med = meds.find { it.id == schedule.medicationId }
                        if (med != null && schedule.isEnabled && schedule.reminderEnabled) {
                            NotificationHelper.scheduleDosageAlarm(
                                context = context,
                                schedule = schedule.toDomain(),
                                medicationName = med.name,
                                dosage = med.dosage
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
