package com.sora.medsman.data.model

data class UserProfile(
    val id: Long = 1,
    val name: String = "",
    val age: Int = 0,
    val email: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
    val allergies: List<String> = emptyList(),
    val medicalConditions: List<String> = emptyList(),
    val bloodType: String = "",
    val dateOfBirth: String = "",
    val healthNotes: String = ""
)
