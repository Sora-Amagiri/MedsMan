package com.sora.medsman.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

data class DailyScheduleItem(
    val schedule: Schedule,
    val medicationName: String,
    val dosage: String,
    val colorHex: String,
    val todayStatus: TakeStatus? = null,
    val isDueToday: Boolean = true
)

sealed interface ScheduleUiState {
    object Loading : ScheduleUiState
    data class Success(
        val todaySchedules: List<DailyScheduleItem>,
        val allSchedules: List<Schedule>
    ) : ScheduleUiState
    data class Error(val message: String) : ScheduleUiState
}

class ScheduleViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    val uiState: StateFlow<ScheduleUiState> = combine(
        repository.schedules,
        repository.medications,
        repository.historyLogs
    ) { schedules, medications, historyLogs ->

        val calendar = Calendar.getInstance()
        val currentCalDay = calendar.get(Calendar.DAY_OF_WEEK)
        // Convert to 1=Mon .. 7=Sun
        val currentModelDay = if (currentCalDay == Calendar.SUNDAY) 7 else currentCalDay - 1

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val todayLogs = historyLogs.filter { it.scheduledTime in todayStart until todayEnd }

        val todayItems = schedules.mapNotNull { schedule ->
            val med = medications.find { it.id == schedule.medicationId } ?: return@mapNotNull null
            val isDue = schedule.isDaily || schedule.daysOfWeek.contains(currentModelDay)

            val loggedStatus = todayLogs
                .filter { it.medicationId == schedule.medicationId }
                .find { log -> isSameTimeOfDay(log.scheduledTime, schedule.timeOfDay) }
                ?.status

            DailyScheduleItem(
                schedule = schedule,
                medicationName = med.name,
                dosage = med.dosage,
                colorHex = med.colorHex,
                todayStatus = loggedStatus,
                isDueToday = isDue
            )
        }
            .filter { it.schedule.isEnabled && it.isDueToday }
            .sortedBy { it.schedule.timeOfDay }

        ScheduleUiState.Success(
            todaySchedules = todayItems,
            allSchedules = schedules
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState.Loading
    )

    fun recordDoseAction(
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: Long,
        status: TakeStatus,
        notes: String? = null,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val inventoryBefore = if (context != null && status == TakeStatus.TAKEN) {
                repository.getInventory(medicationId).firstOrNull()
            } else {
                null
            }

            repository.logDoseAction(
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                scheduledTime = scheduledTime,
                status = status,
                notes = notes
            )

            if (context != null && status == TakeStatus.TAKEN && inventoryBefore != null) {
                val inventoryAfter = repository.getInventory(medicationId).firstOrNull()
                if (inventoryAfter != null &&
                    inventoryAfter.refillReminderEnabled &&
                    inventoryAfter.currentStock <= inventoryAfter.refillThreshold &&
                    inventoryBefore.currentStock > inventoryBefore.refillThreshold
                ) {
                    NotificationHelper.showRefillNotification(
                        context = context,
                        medicationId = medicationId,
                        medicationName = medicationName,
                        currentStock = inventoryAfter.currentStock
                    )
                }
            }
        }
    }

    fun addSchedule(schedule: Schedule, context: Context) {
        viewModelScope.launch {
            val scheduleId = repository.insertSchedule(schedule)
            val med = repository.getMedicationById(schedule.medicationId).firstOrNull()
            if (med != null) {
                NotificationHelper.scheduleDosageAlarm(
                    context = context,
                    schedule = schedule.copy(id = scheduleId),
                    medicationName = med.name,
                    dosage = med.dosage
                )
            }
        }
    }

    fun toggleSchedule(schedule: Schedule, isEnabled: Boolean, context: Context) {
        viewModelScope.launch {
            val updated = schedule.copy(isEnabled = isEnabled)
            repository.updateSchedule(updated)
            if (isEnabled) {
                val med = repository.getMedicationById(schedule.medicationId).firstOrNull()
                if (med != null) {
                    NotificationHelper.scheduleDosageAlarm(
                        context = context,
                        schedule = updated,
                        medicationName = med.name,
                        dosage = med.dosage
                    )
                }
            } else {
                NotificationHelper.cancelDosageAlarm(context, schedule.id)
            }
        }
    }

    fun deleteSchedule(id: Long, context: Context) {
        viewModelScope.launch {
            NotificationHelper.cancelDosageAlarm(context, id)
            repository.deleteSchedule(id)
        }
    }

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(repository) as T
        }
    }
}

private fun isSameTimeOfDay(epochMillis: Long, timeOfDay: String): Boolean {
    val parts = timeOfDay.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return false
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMillis }
    return calendar.get(Calendar.HOUR_OF_DAY) == hour &&
        calendar.get(Calendar.MINUTE) == minute
}
