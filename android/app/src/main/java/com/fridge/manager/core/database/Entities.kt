package com.fridge.manager.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_locations")
data class LocationEntity(
    @PrimaryKey val id: Long,
    val householdId: Long,
    val parentId: Long?,
    val name: String,
    val storageType: String,
    val icon: String?,
    val sortOrder: Int,
    val depth: Int,
    val fullPath: String,
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Long,
    val householdId: Long,
    val locationId: Long,
    val locationPath: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val initialQuantity: Double,
    val unit: String,
    val expiryDate: String?,
    val expiryType: String,
    val status: String,
    val memo: String?,
    val detailImageKey: String? = null,
    val thumbImageKey: String? = null,
    val detailImageUrl: String? = null,
    val thumbImageUrl: String? = null,
    val updatedAt: String,
)
