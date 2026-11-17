package com.sora.medsman.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sora.medsman.data.local.dao.*
import com.sora.medsman.data.local.entity.*

@Database(
    entities = [
        MedicationEntity::class,
        ScheduleEntity::class,
        InventoryEntity::class,
        HistoryLogEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MedsManDatabase : RoomDatabase() {

    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun historyLogDao(): HistoryLogDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: MedsManDatabase? = null

        /**
         * v1 shipped with demo medications, schedules, inventory, history, and a
         * sample profile. v2 starts empty. This migration removes only rows that
         * exactly match the bundled demo data so any real records the user added
         * are preserved. Schedules and inventory cascade from the medication rows.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val sampleMedicationFilter = """
                    (name = 'Lisinopril' AND dosage = '10 mg'
                        AND colorHex = '#2196F3' AND notes = 'For blood pressure control')
                    OR (name = 'Vitamin D3' AND dosage = '2000 IU'
                        AND colorHex = '#FF9800' AND notes = 'Daily supplement')
                    OR (name = 'Ibuprofen' AND dosage = '400 mg'
                        AND colorHex = '#E91E63' AND notes = 'Max 3 times daily')
                """.trimIndent()

                db.execSQL(
                    "DELETE FROM history_logs WHERE medicationId IN " +
                        "(SELECT id FROM medications WHERE $sampleMedicationFilter)"
                )
                db.execSQL(
                    "DELETE FROM schedules WHERE medicationId IN " +
                        "(SELECT id FROM medications WHERE $sampleMedicationFilter)"
                )
                db.execSQL(
                    "DELETE FROM inventory WHERE medicationId IN " +
                        "(SELECT id FROM medications WHERE $sampleMedicationFilter)"
                )
                db.execSQL(
                    "DELETE FROM medications WHERE $sampleMedicationFilter"
                )
                db.execSQL(
                    "DELETE FROM user_profile WHERE id = 1 " +
                        "AND name = 'Jane Doe' AND email = 'jane.doe@example.com'"
                )
            }
        }

        fun getDatabase(context: Context): MedsManDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MedsManDatabase::class.java,
                    "medsman_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
