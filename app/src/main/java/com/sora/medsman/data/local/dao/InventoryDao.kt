package com.sora.medsman.data.local.dao

import androidx.room.*
import com.sora.medsman.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT * FROM inventory WHERE medicationId = :medicationId")
    fun getInventoryForMedication(medicationId: Long): Flow<InventoryEntity?>

    @Query("SELECT * FROM inventory WHERE medicationId = :medicationId")
    suspend fun getInventoryForMedicationSync(medicationId: Long): InventoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInventory(inventory: InventoryEntity)

    @Query("UPDATE inventory SET currentStock = :newStock WHERE medicationId = :medicationId")
    suspend fun updateStock(medicationId: Long, newStock: Int)

    @Query("DELETE FROM inventory WHERE medicationId = :medicationId")
    suspend fun deleteInventory(medicationId: Long)
}
