package com.sora.medsman.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.repository.MedsManRepository
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale

data class DashboardSummary(
    val userName: String = "User",
    val activeMedicationsCount: Int = 0,
    val todayScheduledCount: Int = 0,
    val todayTakenCount: Int = 0,
    val lowStockAlertCount: Int = 0,
    val nextUpcomingSchedule: Schedule? = null,
    val nextMedicationName: String? = null
)

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val summary: DashboardSummary) : DashboardUiState
}

class MedsManMainViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.medications,
        repository.schedules,
        repository.inventoryList,
        repository.historyLogs,
        repository.userProfile
    ) { meds, schedules, invs, historyLogs, profile ->

        val activeMeds = meds.size
        val lowStockCount = invs.count { it.currentStock <= it.refillThreshold }

        val calendar = Calendar.getInstance()
        val currentCalDay = calendar.get(Calendar.DAY_OF_WEEK)
        val currentModelDay = if (currentCalDay == Calendar.SUNDAY) 7 else currentCalDay - 1

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val todayLogs = historyLogs.filter { it.scheduledTime in todayStart until todayEnd }
        val takenCount = todayLogs.count { it.status == TakeStatus.TAKEN }

        val todayDueSchedules = schedules.filter { s ->
            s.isEnabled && (s.isDaily || s.daysOfWeek.contains(currentModelDay))
        }

        val currentHHmm = String.format(
            Locale.US,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )

        val upcomingSchedule = todayDueSchedules
            .filter { it.timeOfDay >= currentHHmm }
            .minByOrNull { it.timeOfDay }
            ?: todayDueSchedules.minByOrNull { it.timeOfDay }

        val upcomingMedName = upcomingSchedule?.let { sched ->
            meds.find { it.id == sched.medicationId }?.name
        }

        DashboardUiState.Success(
            summary = DashboardSummary(
                userName = profile?.name?.ifBlank { "User" } ?: "User",
                activeMedicationsCount = activeMeds,
                todayScheduledCount = todayDueSchedules.size,
                todayTakenCount = takenCount,
                lowStockAlertCount = lowStockCount,
                nextUpcomingSchedule = upcomingSchedule,
                nextMedicationName = upcomingMedName
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedsManMainViewModel(repository) as T
        }
    }
}
