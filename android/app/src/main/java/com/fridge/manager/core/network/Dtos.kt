package com.fridge.manager.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String,
    @SerialName("registration_code") val registrationCode: String,
)

@Serializable
data class ApiErrorBody(
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ApiErrorEnvelope(
    val error: ApiErrorBody? = null,
)

@Serializable
data class DevLoginRequest(
    val nickname: String,
    val email: String? = null,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class UserSummaryDto(
    val id: Long,
    val nickname: String,
    val email: String? = null,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int,
    val user: UserSummaryDto,
)

@Serializable
data class UserHouseholdBriefDto(
    val id: Long,
    val name: String,
    val role: String,
)

@Serializable
data class UserMeDto(
    val id: Long,
    val nickname: String,
    val email: String? = null,
    val households: List<UserHouseholdBriefDto> = emptyList(),
)

@Serializable
data class CreateHouseholdRequest(
    val name: String,
    @SerialName("create_default_locations") val createDefaultLocations: Boolean = true,
)

@Serializable
data class JoinHouseholdRequest(
    val code: String,
)

@Serializable
data class HouseholdDto(
    val id: Long,
    val name: String,
    val role: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class HouseholdMemberDto(
    @SerialName("user_id") val userId: Long,
    val nickname: String,
    val email: String? = null,
    val role: String,
    @SerialName("joined_at") val joinedAt: String,
)

@Serializable
data class CreateInviteRequest(
    @SerialName("expires_in_hours") val expiresInHours: Int = 24,
    @SerialName("max_uses") val maxUses: Int = 5,
)

@Serializable
data class InviteCodeDto(
    val code: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("max_uses") val maxUses: Int,
    @SerialName("used_count") val usedCount: Int,
)

@Serializable
data class CreateLocationRequest(
    @SerialName("parent_id") val parentId: Long? = null,
    val name: String,
    @SerialName("storage_type") val storageType: String = "CUSTOM",
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class LocationNodeDto(
    val id: Long,
    @SerialName("household_id") val householdId: Long,
    @SerialName("parent_id") val parentId: Long? = null,
    val name: String,
    @SerialName("storage_type") val storageType: String,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val depth: Int = 0,
    @SerialName("full_path") val fullPath: String = "",
    val children: List<LocationNodeDto> = emptyList(),
)

@Serializable
data class CreateItemRequest(
    @SerialName("location_id") val locationId: Long,
    val name: String,
    val category: String = "ETC",
    val quantity: Double = 1.0,
    val unit: String = "개",
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("expiry_type") val expiryType: String = "USE_BY",
    val memo: String? = null,
    @SerialName("detail_image_key") val detailImageKey: String? = null,
    @SerialName("thumb_image_key") val thumbImageKey: String? = null,
)

@Serializable
data class UpdateItemRequest(
    @SerialName("location_id") val locationId: Long? = null,
    val name: String? = null,
    val quantity: Double? = null,
    val memo: String? = null,
)

@Serializable
data class ConsumeItemRequest(
    val amount: Double? = null,
    @SerialName("remaining_quantity") val remainingQuantity: Double? = null,
    val note: String? = null,
)

@Serializable
data class ItemDto(
    val id: Long,
    @SerialName("household_id") val householdId: Long,
    @SerialName("location_id") val locationId: Long,
    @SerialName("location_path") val locationPath: String,
    val name: String,
    val category: String,
    val quantity: Double,
    @SerialName("initial_quantity") val initialQuantity: Double? = null,
    val unit: String,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("expiry_type") val expiryType: String,
    @SerialName("d_day") val dDay: Int? = null,
    @SerialName("expiry_state") val expiryState: String,
    val status: String,
    val memo: String? = null,
    @SerialName("detail_image_key") val detailImageKey: String? = null,
    @SerialName("thumb_image_key") val thumbImageKey: String? = null,
    @SerialName("detail_image_url") val detailImageUrl: String? = null,
    @SerialName("thumb_image_url") val thumbImageUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class PhotoUploadUrlRequest(
    @SerialName("detail_size_bytes") val detailSizeBytes: Int,
    @SerialName("thumb_size_bytes") val thumbSizeBytes: Int,
    @SerialName("content_type") val contentType: String = "image/webp",
)

@Serializable
data class PhotoUploadUrlResponse(
    @SerialName("detail_image_key") val detailImageKey: String,
    @SerialName("thumb_image_key") val thumbImageKey: String,
    @SerialName("detail_upload_url") val detailUploadUrl: String,
    @SerialName("thumb_upload_url") val thumbUploadUrl: String,
    @SerialName("expires_in_seconds") val expiresInSeconds: Int = 300,
)

@Serializable
data class ActivityLogDto(
    val id: Long,
    @SerialName("household_id") val householdId: Long,
    @SerialName("actor_user_id") val actorUserId: Long,
    @SerialName("actor_nickname") val actorNickname: String,
    @SerialName("entity_type") val entityType: String,
    @SerialName("entity_id") val entityId: Long,
    @SerialName("action_type") val actionType: String,
    val detail: Map<String, JsonElement>? = null,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ActivityListDto(
    val items: List<ActivityLogDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: Long? = null,
)

@Serializable
data class HealthResponseDto(
    val status: String,
    @SerialName("db_provider") val dbProvider: String? = null,
    val detail: String? = null,
    val maintenance: Boolean = false,
    @SerialName("maintenance_message") val maintenanceMessage: String? = null,
    @SerialName("min_android_version_code") val minAndroidVersionCode: Int = 1,
)
