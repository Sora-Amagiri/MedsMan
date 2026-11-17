package com.sora.medsman.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sora.medsman.data.model.Schedule

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val timeOfDay: String,
    val daysOfWeek: List<Int>,
    val isDaily: Boolean,
    val dosageQuantity: String,
    val isEnabled: Boolean,
    val reminderEnabled: Boolean
) {
    fun toDomain(): Schedule = Schedule(
        id = id,
        medicationId = medicationId,
        timeOfDay = timeOfDay,
        daysOfWeek = daysOfWeek,
        isDaily = isDaily,
        dosageQuantity = dosageQuantity,
        isEnabled = isEnabled,
        reminderEnabled = reminderEnabled
    )

    companion object {
        fun fromDomain(domain: Schedule): ScheduleEntity = ScheduleEntity(
            id = domain.id,
            medicationId = domain.medicationId,
            timeOfDay = domain.timeOfDay,
            daysOfWeek = domain.daysOfWeek,
            isDaily = domain.isDaily,
            dosageQuantity = domain.dosageQuantity,
            isEnabled = domain.isEnabled,
            reminderEnabled = domain.reminderEnabled
        )
    }
}
