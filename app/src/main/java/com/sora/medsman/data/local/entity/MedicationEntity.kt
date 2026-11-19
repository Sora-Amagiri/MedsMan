package com.sora.medsman.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sora.medsman.data.model.Medication

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    @ColumnInfo(defaultValue = "1")
    val unitsPerDose: Int = 1,
    val form: String,
    val instructions: String,
    val category: String,
    val colorHex: String,
    val shapeIcon: String,
    val isPRN: Boolean,
    val notes: String,
    val createdAt: Long
) {
    fun toDomain(): Medication = Medication(
        id = id,
        name = name,
        dosage = dosage,
        unitsPerDose = unitsPerDose,
        form = form,
        instructions = instructions,
        category = category,
        colorHex = colorHex,
        shapeIcon = shapeIcon,
        isPRN = isPRN,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: Medication): MedicationEntity = MedicationEntity(
            id = domain.id,
            name = domain.name,
            dosage = domain.dosage,
            unitsPerDose = domain.unitsPerDose,
            form = domain.form,
            instructions = domain.instructions,
            category = domain.category,
            colorHex = domain.colorHex,
            shapeIcon = domain.shapeIcon,
            isPRN = domain.isPRN,
            notes = domain.notes,
            createdAt = domain.createdAt
        )
    }
}
