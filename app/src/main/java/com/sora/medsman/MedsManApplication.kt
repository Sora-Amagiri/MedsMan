package com.sora.medsman

import android.app.Application
import android.content.Context
import com.sora.medsman.data.local.MedsManDatabase
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.data.repository.MedsManRepositoryImpl
import com.sora.medsman.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedsManApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: MedsManDatabase
        private set

    lateinit var repository: MedsManRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = MedsManDatabase.getDatabase(this)
        repository = MedsManRepositoryImpl(database)
        NotificationHelper.createNotificationChannels(this)
        clearLegacySampleNotificationsOnce()
        scheduleExistingReminders()
    }

    /**
     * v1 preloaded demo data. Cancel any alarms or notifications left over from
     * those sample schedules/medications the first time v2 launches.
     */
    private fun clearLegacySampleNotificationsOnce() {
        val prefs = getSharedPreferences("medsman_cleanup", Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_LEGACY_SAMPLES_CLEARED, false)) return

        (1L..3L).forEach { sampleId ->
            NotificationHelper.cancelDosageAlarm(this, sampleId)
            NotificationHelper.cancelNotification(
                context = this,
                notificationId = (sampleId.toInt() * 100) + 5
            )
        }
        prefs.edit().putBoolean(KEY_LEGACY_SAMPLES_CLEARED, true).apply()
    }

    /**
     * Alarms are one-shot and can be lost when the process dies, the app is
     * updated, or the device is force-stopped. Re-arm every enabled schedule on
     * each app start so reminders keep firing.
     */
    private fun scheduleExistingReminders() {
        applicationScope.launch {
            try {
                NotificationHelper.rescheduleAllReminders(this@MedsManApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private companion object {
        const val KEY_LEGACY_SAMPLES_CLEARED = "legacy_samples_cleared"
    }
}
