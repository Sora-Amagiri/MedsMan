package com.sora.medsman.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sora.medsman.data.model.Inventory
import com.sora.medsman.data.model.Medication
import com.sora.medsman.data.model.Schedule
import com.sora.medsman.data.repository.MedsManRepository
import com.sora.medsman.notification.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryItemWithMed(
    val medication: Medication,
    val inventory: Inventory,
    val isLowStock: Boolean,
    val schedules: List<Schedule> = emptyList()
)

sealed interface InventoryUiState {
    object Loading : InventoryUiState
    data class Success(
        val items: List<InventoryItemWithMed>,
        val lowStockItems: List<InventoryItemWithMed>
    ) : InventoryUiState
    data class Error(val message: String) : InventoryUiState
}

class InventoryViewModel(
    private val repository: MedsManRepository
) : ViewModel() {

    val uiState: StateFlow<InventoryUiState> = combine(
        repository.medications,
        repository.inventoryList,
        repository.schedules
    ) { medications, inventoryList, schedules ->
        val items = medications.map { med ->
            val inv = inventoryList.find { it.medicationId == med.id }
                ?: Inventory(medicationId = med.id)
            InventoryItemWithMed(
                medication = med,
                inventory = inv,
                isLowStock = inv.currentStock <= inv.refillThreshold,
                schedules = schedules.filter { it.medicationId == med.id }
            )
        }

        val lowStock = items.filter { it.isLowStock }

        InventoryUiState.Success(
            items = items,
            lowStockItems = lowStock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState.Loading
    )

    fun refillStock(medicationId: Long, addAmount: Int, context: Context? = null) {
        viewModelScope.launch {
            repository.refillStock(medicationId, addAmount)
            if (context != null) {
                NotificationHelper.cancelNotification(
                    context = context,
                    notificationId = (medicationId.toInt() * 100) + 5
                )
            }
        }
    }

    fun adjustStock(medicationId: Long, delta: Int) {
        viewModelScope.launch {
            repository.adjustStock(medicationId, delta)
        }
    }

    fun updateInventory(inventory: Inventory, context: Context? = null) {
        viewModelScope.launch {
            // Preserve stock that may have changed while the settings sheet was open.
            val current = repository.getInventory(inventory.medicationId).firstOrNull()
            val wasLowStock = current != null &&
                current.currentStock <= current.refillThreshold
            val merged = (current ?: inventory).copy(
                refillThreshold = inventory.refillThreshold,
                refillReminderEnabled = inventory.refillReminderEnabled,
                pharmacyName = inventory.pharmacyName,
                pharmacyPhone = inventory.pharmacyPhone
            )
            repository.updateInventory(merged)
            if (context != null &&
                !wasLowStock &&
                merged.refillReminderEnabled &&
                merged.currentStock <= merged.refillThreshold
            ) {
                val med = repository.getMedicationById(merged.medicationId).firstOrNull()
                if (med != null) {
                    NotificationHelper.showRefillNotification(
                        context = context,
                        medicationId = med.id,
                        medicationName = med.name,
                        currentStock = merged.currentStock
                    )
                }
            }
        }
    }

    fun saveMedication(
        existing: InventoryItemWithMed?,
        medication: Medication,
        inventory: Inventory,
        scheduleTimes: List<String>,
        context: Context
    ) {
        viewModelScope.launch {
            if (existing == null) {
                val medicationId = repository.insertMedication(
                    medication = medication,
                    inventory = inventory,
                    schedules = scheduleTimes.distinct().sorted().map { time ->
                        Schedule(
                            medicationId = 0,
                            timeOfDay = time,
                            dosageQuantity = medication.unitsPerDose.toString()
                        )
                    }
                )
                repository.getSchedulesForMedication(medicationId).first().forEach { schedule ->
                    NotificationHelper.scheduleDosageAlarm(
                        context = context,
                        schedule = schedule,
                        medicationName = medication.name,
                        dosage = medication.dosage
                    )
                }
            } else {
                val medicationId = existing.medication.id
                val updatedMedication = medication.copy(id = medicationId)
                repository.updateMedication(updatedMedication)

                val currentInventory = repository.getInventory(medicationId).firstOrNull()
                val updatedInventory = (currentInventory ?: inventory).copy(
                    medicationId = medicationId,
                    currentStock = inventory.currentStock,
                    unit = inventory.unit,
                    refillThreshold = inventory.refillThreshold,
                    refillReminderEnabled = inventory.refillReminderEnabled,
                    lastRefillDate = if (currentInventory != null &&
                        inventory.currentStock > currentInventory.currentStock
                    ) {
                        System.currentTimeMillis()
                    } else {
                        currentInventory?.lastRefillDate ?: inventory.lastRefillDate
                    }
                )
                repository.updateInventory(updatedInventory)

                if (updatedInventory.currentStock > updatedInventory.refillThreshold) {
                    NotificationHelper.cancelNotification(
                        context = context,
                        notificationId = (medicationId.toInt() * 100) + 5
                    )
                }

                val desiredTimes = scheduleTimes.distinct().sorted()
                val existingSchedules = repository.getSchedulesForMedication(medicationId).first()

                existingSchedules
                    .filter { it.timeOfDay !in desiredTimes }
                    .forEach { schedule ->
                        NotificationHelper.cancelDosageAlarm(context, schedule.id)
                        repository.deleteSchedule(schedule.id)
                    }

                val schedulesByTime = existingSchedules.associateBy { it.timeOfDay }
                desiredTimes.forEach { time ->
                    val current = schedulesByTime[time]
                    val schedule = current?.copy(
                        isEnabled = true,
                        reminderEnabled = true,
                        dosageQuantity = updatedMedication.unitsPerDose.toString()
                    ) ?: Schedule(
                        medicationId = medicationId,
                        timeOfDay = time,
                        dosageQuantity = updatedMedication.unitsPerDose.toString()
                    )
                    val scheduleId = if (current != null) {
                        repository.updateSchedule(schedule)
                        schedule.id
                    } else {
                        repository.insertSchedule(schedule)
                    }
                    NotificationHelper.scheduleDosageAlarm(
                        context = context,
                        schedule = schedule.copy(id = scheduleId),
                        medicationName = updatedMedication.name,
                        dosage = updatedMedication.dosage
                    )
                }
            }
        }
    }

    fun deleteMedication(item: InventoryItemWithMed, context: Context) {
        viewModelScope.launch {
            item.schedules.forEach { schedule ->
                NotificationHelper.cancelDosageAlarm(context, schedule.id)
            }
            NotificationHelper.cancelNotification(
                context = context,
                notificationId = (item.medication.id.toInt() * 100) + 5
            )
            repository.deleteMedication(item.medication.id)
        }
    }

    class Factory(private val repository: MedsManRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return InventoryViewModel(repository) as T
        }
    }
}
