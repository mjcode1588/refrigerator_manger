package com.fridge.manager.domain.model

enum class ExpiryState {
    EXPIRED,
    TODAY,
    EXPIRING_SOON,
    SAFE,
    NONE,
}

data class HouseholdInfo(
    val id: Long,
    val name: String,
    val role: String,
)

data class StorageLocationNode(
    val id: Long,
    val householdId: Long,
    val parentId: Long?,
    val name: String,
    val storageType: String,
    val icon: String?,
    val sortOrder: Int,
    val depth: Int,
    val fullPath: String,
    val children: List<StorageLocationNode> = emptyList(),
)

data class FoodItem(
    val id: Long,
    val householdId: Long,
    val locationId: Long,
    val locationPath: String,
    val name: String,
    val category: String,
    val quantity: Double,
    val initialQuantity: Double = quantity,
    val unit: String,
    val expiryDate: String?,
    val expiryType: String,
    val dDay: Int?,
    val expiryState: ExpiryState,
    val status: String,
    val memo: String?,
    val detailImageUrl: String? = null,
    val thumbImageUrl: String? = null,
)

data class ActivityLogItem(
    val id: Long,
    val actorNickname: String,
    val entityType: String,
    val actionType: String,
    val summaryName: String?,
    val createdAt: String,
)

data class ShoppingItem(
    val id: Long,
    val householdId: Long,
    val name: String,
    val category: String,
    val quantity: Double,
    val unit: String,
    val memo: String?,
    val isPurchased: Boolean,
    val purchasedBy: Long?,
    val purchasedByNickname: String?,
    val purchasedAt: String?,
    val createdBy: Long,
    val createdAt: String,
    val updatedAt: String,
)
