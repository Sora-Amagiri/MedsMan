package com.sora.medsman.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sora.medsman.data.model.HistoryLog
import com.sora.medsman.data.model.TakeStatus

@Entity(
    tableName = "history_logs",
    indices = [Index("medicationId"), Index("scheduledTime")]
)
data class HistoryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    @ColumnInfo(defaultValue = "1")
    val unitsPerDose: Int = 1,
    val scheduledTime: Long,
    val actionTime: Long,
    val status: TakeStatus,
    val notes: String?
) {
    fun toDomain(): HistoryLog = HistoryLog(
        id = id,
        medicationId = medicationId,
        medicationName = medicationName,
        dosage = dosage,
        unitsPerDose = unitsPerDose,
        scheduledTime = scheduledTime,
        actionTime = actionTime,
        status = status,
        notes = notes
    )

    companion object {
        fun fromDomain(domain: HistoryLog): HistoryLogEntity = HistoryLogEntity(
            id = domain.id,
            medicationId = domain.medicationId,
            medicationName = domain.medicationName,
            dosage = domain.dosage,
            unitsPerDose = domain.unitsPerDose,
            scheduledTime = domain.scheduledTime,
            actionTime = domain.actionTime,
            status = domain.status,
            notes = domain.notes
        )
    }
}
