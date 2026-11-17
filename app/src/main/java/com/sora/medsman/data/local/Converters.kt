package com.sora.medsman.data.local

import androidx.room.TypeConverter
import com.sora.medsman.data.model.TakeStatus

class Converters {

    @TypeConverter
    fun fromIntList(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toIntList(data: String?): List<Int> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(";;") ?: ""
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";;").map { it.trim() }.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromTakeStatus(status: TakeStatus?): String {
        return status?.name ?: TakeStatus.TAKEN.name
    }

    @TypeConverter
    fun toTakeStatus(value: String?): TakeStatus {
        return try {
            if (value != null) TakeStatus.valueOf(value) else TakeStatus.TAKEN
        } catch (e: Exception) {
            TakeStatus.TAKEN
        }
    }
}
