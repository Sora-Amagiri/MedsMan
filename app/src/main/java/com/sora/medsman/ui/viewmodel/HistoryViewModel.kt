package com.sora.medsman.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.HistoryLog
import com.sora.medsman.data.model.TakeStatus
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AdherenceStats(
    val totalDoses: Int = 0,
    val takenCount: Int = 0,
    val skippedCount: Int = 0,
    val missedCount: Int = 0,
    val snoozedCount: Int = 0,
    val adherencePercentage: Int = 0
)

sealed interface HistoryUiState {
    object Loading : HistoryUiState
    data class Success(
        val historyLogs: List<HistoryLog>,
        val stats: AdherenceStats
    ) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

class HistoryViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    private val _filterStatus = MutableStateFlow<TakeStatus?>(null)
    val filterStatus: StateFlow<TakeStatus?> = _filterStatus.asStateFlow()

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.historyLogs,
        _filterStatus
    ) { logs, filter ->
        val filtered = if (filter != null) {
            logs.filter { it.status == filter }
        } else {
            logs
        }

        val total = logs.size
        val taken = logs.count { it.status == TakeStatus.TAKEN }
        val skipped = logs.count { it.status == TakeStatus.SKIPPED }
        val missed = logs.count { it.status == TakeStatus.MISSED }
        val snoozed = logs.count { it.status == TakeStatus.SNOOZED }

        val pct = if (total > 0) ((taken.toDouble() / total.toDouble()) * 100).toInt() else 0

        val stats = AdherenceStats(
            totalDoses = total,
            takenCount = taken,
            skippedCount = skipped,
            missedCount = missed,
            snoozedCount = snoozed,
            adherencePercentage = pct
        )

        HistoryUiState.Success(
            historyLogs = filtered,
            stats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState.Loading
    )

    fun setFilterStatus(status: TakeStatus?) {
        _filterStatus.value = status
    }

    fun deleteLog(id: Long, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteHistoryLog(id)
            if (context != null) {
                NotificationHelper.rescheduleAllReminders(context)
            }
        }
    }

    fun clearAllHistory(context: Context? = null) {
        viewModelScope.launch {
            repository.clearHistory()
            if (context != null) {
                NotificationHelper.rescheduleAllReminders(context)
            }
        }
    }

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
