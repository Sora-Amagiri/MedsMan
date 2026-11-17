package com.sora.medsman.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.sora.medsman.data.model.Inventory

@Entity(
    tableName = "inventory",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InventoryEntity(
    @PrimaryKey
    val medicationId: Long,
    val currentStock: Int,
    val unit: String,
    val refillThreshold: Int,
    val refillReminderEnabled: Boolean,
    val lastRefillDate: Long?,
    val pharmacyName: String,
    val pharmacyPhone: String,
    val rxNumber: String
) {
    fun toDomain(): Inventory = Inventory(
        medicationId = medicationId,
        currentStock = currentStock,
        unit = unit,
        refillThreshold = refillThreshold,
        refillReminderEnabled = refillReminderEnabled,
        lastRefillDate = lastRefillDate,
        pharmacyName = pharmacyName,
        pharmacyPhone = pharmacyPhone,
        rxNumber = rxNumber
    )

    companion object {
        fun fromDomain(domain: Inventory): InventoryEntity = InventoryEntity(
            medicationId = domain.medicationId,
            currentStock = domain.currentStock,
            unit = domain.unit,
            refillThreshold = domain.refillThreshold,
            refillReminderEnabled = domain.refillReminderEnabled,
            lastRefillDate = domain.lastRefillDate,
            pharmacyName = domain.pharmacyName,
            pharmacyPhone = domain.pharmacyPhone,
            rxNumber = domain.rxNumber
        )
    }
}
