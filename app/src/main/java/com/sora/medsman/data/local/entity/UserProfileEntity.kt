package com.sora.medsman.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sora.medsman.data.model.UserProfile

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val name: String,
    val age: Int = 0,
    val email: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val doctorName: String,
    val doctorPhone: String,
    val allergies: List<String>,
    val medicalConditions: List<String>,
    val bloodType: String,
    val dateOfBirth: String,
    val healthNotes: String = ""
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        name = name,
        age = age,
        email = email,
        emergencyContactName = emergencyContactName,
        emergencyContactPhone = emergencyContactPhone,
        doctorName = doctorName,
        doctorPhone = doctorPhone,
        allergies = allergies,
        medicalConditions = medicalConditions,
        bloodType = bloodType,
        dateOfBirth = dateOfBirth,
        healthNotes = healthNotes
    )

    companion object {
        fun fromDomain(domain: UserProfile): UserProfileEntity = UserProfileEntity(
            id = domain.id,
            name = domain.name,
            age = domain.age,
            email = domain.email,
            emergencyContactName = domain.emergencyContactName,
            emergencyContactPhone = domain.emergencyContactPhone,
            doctorName = domain.doctorName,
            doctorPhone = domain.doctorPhone,
            allergies = domain.allergies,
            medicalConditions = domain.medicalConditions,
            bloodType = domain.bloodType,
            dateOfBirth = domain.dateOfBirth,
            healthNotes = domain.healthNotes
        )
    }
}
