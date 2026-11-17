package com.sora.medsman.data.model

data class Schedule(
    val id: Long = 0,
    val medicationId: Long,
    val timeOfDay: String, // "08:00", "20:00"
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon .. 7=Sun
    val isDaily: Boolean = true,
    val dosageQuantity: String = "1",
    val isEnabled: Boolean = true,
    val reminderEnabled: Boolean = true
)
