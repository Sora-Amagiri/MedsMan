package com.sora.medsman.data.repository

import com.sora.medsman.data.model.*
import kotlinx.coroutines.flow.Flow

interface MedsManRepository {
    val medications: Flow<List<Medication>>
    val medicationsWithDetails: Flow<List<MedicationWithDetails>>
    val schedules: Flow<List<Schedule>>
    val inventoryList: Flow<List<Inventory>>
    val historyLogs: Flow<List<HistoryLog>>
    val userProfile: Flow<UserProfile?>

    fun getMedicationById(id: Long): Flow<Medication?>
    fun getMedicationWithDetails(id: Long): Flow<MedicationWithDetails?>
    suspend fun insertMedication(
        medication: Medication,
        inventory: Inventory? = null,
        schedules: List<Schedule> = emptyList()
    ): Long

    suspend fun updateMedication(medication: Medication)
    suspend fun deleteMedication(id: Long)

    fun getSchedulesForMedication(medicationId: Long): Flow<List<Schedule>>
    suspend fun insertSchedule(schedule: Schedule): Long
    suspend fun updateSchedule(schedule: Schedule)
    suspend fun deleteSchedule(id: Long)

    fun getInventory(medicationId: Long): Flow<Inventory?>
    suspend fun updateInventory(inventory: Inventory)
    suspend fun refillStock(medicationId: Long, addAmount: Int)
    suspend fun decrementStock(medicationId: Long, amount: Int = 1): Int
    suspend fun adjustStock(medicationId: Long, delta: Int): Int

    suspend fun logDoseAction(
        medicationId: Long,
        medicationName: String,
        dosage: String,
        scheduledTime: Long,
        status: TakeStatus,
        notes: String? = null
    )

    suspend fun clearHistory()
    suspend fun deleteHistoryLog(id: Long)

    suspend fun updateUserProfile(profile: UserProfile)
}
