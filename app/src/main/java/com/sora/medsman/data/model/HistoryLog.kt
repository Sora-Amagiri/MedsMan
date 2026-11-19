package com.sora.medsman.data.model

data class HistoryLog(
    val id: Long = 0,
    val medicationId: Long,
    val medicationName: String,
    val dosage: String,
    val unitsPerDose: Int = 1,
    val scheduledTime: Long,
    val actionTime: Long = System.currentTimeMillis(),
    val status: TakeStatus,
    val notes: String? = null
)
