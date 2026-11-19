package com.sora.medsman.data.repository

import com.sora.medsman.data.local.MedsManDatabase
import com.sora.medsman.data.local.entity.*
import com.sora.medsman.data.model.*
import com.sora.medsman.util.stockDeltaForStatusChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MedsManRepositoryImpl(
    database: MedsManDatabase
) : MedsManRepository {

    private val medicationDao = database.medicationDao()
    private val scheduleDao = database.scheduleDao()
    private val inventoryDao = database.inventoryDao()
    private val historyLogDao = database.historyLogDao()
    private val userProfileDao = database.userProfileDao()

    override val medications: Flow<List<Medication>> =
        medicationDao.getAllMedications().map { entities ->
            entities.map { it.toDomain() }
        }

    override val schedules: Flow<List<Schedule>> =
        scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toDomain() }
        }

    override val inventoryList: Flow<List<Inventory>> =
        inventoryDao.getAllInventory().map { entities ->
            entities.map { it.toDomain() }
        }

    override val historyLogs: Flow<List<HistoryLog>> =
        historyLogDao.getAllHistoryLogs().map { entities ->
            entities.map { it.toDomain() }
        }

    override val userProfile: Flow<UserProfile?> =
        userProfileDao.getUserProfile().map { entity ->
            entity?.toDomain()
        }

    override val medicationsWithDetails: Flow<List<MedicationWithDetails>> =
        combine(medications, inventoryList, schedules) { meds, invs, scheds ->
            meds.map { med ->
                val inv = invs.find { it.medicationId == med.id }
                val medScheds = scheds.filter { it.medicationId == med.id }
                MedicationWithDetails(
                    medication = med,
                    inventory = inv,
                    schedules = medScheds
                )
            }
        }

    override fun getMedicationById(id: Long): Flow<Medication?> =
        medicationDao.getMedicationById(id).map { it?.toDomain() }

    override fun getMedicationWithDetails(id: Long): Flow<MedicationWithDetails?> =
        combine(
            getMedicationById(id),
            inventoryDao.getInventoryForMedication(id).map { it?.toDomain() },
            scheduleDao.getSchedulesForMedication(id).map { list -> list.map { it.toDomain() } }
        ) { med, inv, scheds ->
            if (med == null) null
            else MedicationWithDetails(medication = med, inventory = inv, schedules = scheds)
        }

    override suspend fun insertMedication(
        medication: Medication,
        inventory: Inventory?,
        schedules: List<Schedule>
    ): Long {
        val medId = medicationDao.insertMedication(MedicationEntity.fromDomain(medication))

        inventory?.let { inv ->
            inventoryDao.insertOrUpdateInventory(
                InventoryEntity.fromDomain(inv.copy(medicationId = medId))
            )
        }

        if (schedules.isNotEmpty()) {
            val scheduleEntities = schedules.map {
                ScheduleEntity.fromDomain(it.copy(medicationId = medId))
            }
            scheduleDao.insertSchedules(scheduleEntities)
        }

        return medId
    }

    override suspend fun updateMedication(medication: Medication) {
        medicationDao.updateMedication(MedicationEntity.fromDomain(medication))
    }

    override suspend fun deleteMedication(id: Long) {
        medicationDao.deleteMedicationById(id)
        // Foreign keys with CASCADE will remove schedules and inventory automatically
    }

    override fun getSchedulesForMedication(medicationId: Long): Flow<List<Schedule>> =
        scheduleDao.getSchedulesForMedication(medicationId).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertSchedule(schedule: Schedule): Long =
        scheduleDao.insertSchedule(ScheduleEntity.fromDomain(schedule))

    override suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.updateSchedule(ScheduleEntity.fromDomain(schedule))
    }

    override suspend fun deleteSchedule(id: Long) {
        scheduleDao.deleteScheduleById(id)
    }

    override fun getInventory(medicationId: Long): Flow<Inventory?> =
        inventoryDao.getInventoryForMedication(medicationId).map { it?.toDomain() }

    override suspend fun updateInventory(inventory: Inventory) {
        stockMutex.withLock {
            inventoryDao.insertOrUpdateInventory(InventoryEntity.fromDomain(inventory))
        }
    }

    override suspend fun refillStock(medicationId: Long, addAmount: Int) {
        stockMutex.withLock {
            val current = inventoryDao.getInventoryForMedicationSync(medicationId)
            if (current != null) {
                val updated = current.copy(
                    currentStock = current.currentStock + addAmount,
                    lastRefillDate = System.currentTimeMillis()
                )
                inventoryDao.insertOrUpdateInventory(updated)
            } else {
                inventoryDao.insertOrUpdateInventory(
                    InventoryEntity(
                        medicationId = medicationId,
                        currentStock = addAmount,
                        unit = "pills",
                        refillThreshold = 5,
                        refillReminderEnabled = true,
                        lastRefillDate = System.currentTimeMillis(),
                        pharmacyName = "",
                        pharmacyPhone = "",
                        rxNumber = ""
                    )
                )
            }
        }
    }

    override suspend fun decrementStock(medicationId: Long, amount: Int): Int =
        stockMutex.withLock {
            decrementStockLocked(medicationId, amount)
        }

    override suspend fun adjustStock(medicationId: Long, delta: Int): Int =
        stockMutex.withLock {
            val current = inventoryDao.getInventoryForMedicationSync(medicationId)
                ?: return@withLock 0
            val newStock = (current.currentStock + delta).coerceAtLeast(0)
            inventoryDao.updateStock(medicationId, newStock)
            newStock
        }

    private suspend fun decrementStockLocked(medicationId: Long, amount: Int): Int {
        val current = inventoryDao.getInventoryForMedicationSync(medicationId)
        if (current != null) {
            val newStock = (current.currentStock - amount).coerceAtLeast(0)
            inventoryDao.updateStock(medicationId, newStock)
            return newStock
        }
        return 0
    }

    override suspend fun logDoseAction(
        medicationId: Long,
        medicationName: String,
        dosage: String,
        unitsPerDose: Int,
        scheduledTime: Long,
        status: TakeStatus,
        notes: String?
    ) {
        val doseUnits = unitsPerDose.coerceAtLeast(1)

        stockMutex.withLock {
            val existing = historyLogDao.getLogForDose(medicationId, scheduledTime)
            val log = HistoryLogEntity(
                id = existing?.id ?: 0,
                medicationId = medicationId,
                medicationName = medicationName,
                dosage = dosage,
                unitsPerDose = doseUnits,
                scheduledTime = scheduledTime,
                actionTime = System.currentTimeMillis(),
                status = status,
                notes = notes ?: existing?.notes
            )

            if (existing == null) {
                historyLogDao.insertHistoryLog(log)
                if (status == TakeStatus.TAKEN) {
                    decrementStockLocked(medicationId, doseUnits)
                }
            } else {
                historyLogDao.updateHistoryLog(log)
                // Keep inventory in sync when the user corrects a previously logged
                // dose, including when the per-dose amount has changed.
                val stockDelta = stockDeltaForStatusChange(
                    existingStatus = existing.status,
                    existingUnitsPerDose = existing.unitsPerDose,
                    newStatus = status,
                    newUnitsPerDose = doseUnits
                )

                if (stockDelta != 0) {
                    val inventory = inventoryDao.getInventoryForMedicationSync(medicationId)
                    if (inventory != null) {
                        inventoryDao.updateStock(
                            medicationId,
                            (inventory.currentStock + stockDelta).coerceAtLeast(0)
                        )
                    }
                }
            }
        }
    }

    override suspend fun clearHistory() {
        historyLogDao.clearAllHistory()
    }

    override suspend fun deleteHistoryLog(id: Long) {
        historyLogDao.deleteHistoryLog(id)
    }

    override suspend fun updateUserProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateUserProfile(UserProfileEntity.fromDomain(profile))
    }

    private companion object {
        /**
         * Serializes read-modify-write stock operations across repository instances
         * (for example, rapid stock stepper taps or an alarm receiver racing the UI).
         */
        val stockMutex = Mutex()
    }
}
