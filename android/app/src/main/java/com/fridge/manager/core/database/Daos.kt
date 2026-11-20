package com.fridge.manager.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {
    @Query("SELECT * FROM storage_locations WHERE householdId = :householdId ORDER BY sortOrder, id")
    fun observeByHousehold(householdId: Long): Flow<List<LocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationEntity>)

    @Query("DELETE FROM storage_locations WHERE householdId = :householdId")
    suspend fun deleteByHousehold(householdId: Long)

    @Transaction
    suspend fun replaceHouseholdLocations(householdId: Long, locations: List<LocationEntity>) {
        deleteByHousehold(householdId)
        insertAll(locations)
    }
}

@Dao
interface ItemDao {
    @Query(
        "SELECT * FROM items WHERE householdId = :householdId AND status = :status " +
            "ORDER BY CASE WHEN expiryDate IS NULL THEN 1 ELSE 0 END, expiryDate ASC, id ASC"
    )
    fun observeItems(householdId: Long, status: String = "ACTIVE"): Flow<List<ItemEntity>>

    @Query(
        "SELECT * FROM items WHERE householdId = :householdId AND status = 'ACTIVE' " +
            "AND expiryDate IS NOT NULL AND expiryDate <= :cutoffDateIso " +
            "ORDER BY expiryDate ASC, id ASC"
    )
    suspend fun getExpiringItems(householdId: Long, cutoffDateIso: String): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ItemEntity)

    @Query("DELETE FROM items WHERE householdId = :householdId AND status = :status")
    suspend fun deleteByHouseholdAndStatus(householdId: Long, status: String)

    @Transaction
    suspend fun replaceHouseholdItems(
        householdId: Long,
        status: String,
        items: List<ItemEntity>,
    ) {
        deleteByHouseholdAndStatus(householdId, status)
        insertAll(items)
    }
}
