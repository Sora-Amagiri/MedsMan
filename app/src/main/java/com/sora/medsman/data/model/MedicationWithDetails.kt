package com.sora.medsman.data.model

data class MedicationWithDetails(
    val medication: Medication,
    val inventory: Inventory? = null,
    val schedules: List<Schedule> = emptyList()
)
