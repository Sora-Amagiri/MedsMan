package com.sora.medsman.data.local.dao

import androidx.room.*
import com.sora.medsman.data.local.entity.HistoryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryLogDao {
    @Query("SELECT * FROM history_logs ORDER BY actionTime DESC")
    fun getAllHistoryLogs(): Flow<List<HistoryLogEntity>>

    @Query("SELECT * FROM history_logs WHERE medicationId = :medicationId ORDER BY actionTime DESC")
    fun getHistoryLogsForMedication(medicationId: Long): Flow<List<HistoryLogEntity>>

    @Query("SELECT * FROM history_logs WHERE scheduledTime BETWEEN :startTime AND :endTime ORDER BY scheduledTime ASC")
    fun getHistoryLogsForDateRange(startTime: Long, endTime: Long): Flow<List<HistoryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryLog(historyLog: HistoryLogEntity): Long

    @Update
    suspend fun updateHistoryLog(historyLog: HistoryLogEntity)

    @Query(
        "SELECT * FROM history_logs WHERE medicationId = :medicationId " +
            "AND scheduledTime = :scheduledTime LIMIT 1"
    )
    suspend fun getLogForDose(medicationId: Long, scheduledTime: Long): HistoryLogEntity?

    @Query("DELETE FROM history_logs WHERE id = :id")
    suspend fun deleteHistoryLog(id: Long)

    @Query("DELETE FROM history_logs")
    suspend fun clearAllHistory()
}
