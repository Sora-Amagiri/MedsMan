package com.sora.medsman.data.model

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val form: String = "Pill",
    val instructions: String = "",
    val category: String = "Prescription",
    val colorHex: String = "#4CAF50",
    val shapeIcon: String = "pill",
    val isPRN: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
