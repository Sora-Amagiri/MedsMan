package com.sora.medsman.data.local.dao

import androidx.room.*
import com.sora.medsman.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    fun getSchedulesForMedication(medicationId: Long): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId")
    suspend fun getSchedulesForMedicationSync(medicationId: Long): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteSchedulesForMedication(medicationId: Long)
}
