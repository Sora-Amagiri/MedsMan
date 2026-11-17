package com.sora.medsman.data.model

data class Inventory(
    val medicationId: Long,
    val currentStock: Int = 0,
    val unit: String = "pills",
    val refillThreshold: Int = 5,
    val refillReminderEnabled: Boolean = true,
    val lastRefillDate: Long? = null,
    val pharmacyName: String = "",
    val pharmacyPhone: String = "",
    val rxNumber: String = ""
)
