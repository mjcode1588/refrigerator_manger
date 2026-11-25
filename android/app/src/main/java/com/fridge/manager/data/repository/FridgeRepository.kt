package com.fridge.manager.data.repository

import com.fridge.manager.BuildConfig
import com.fridge.manager.core.database.ItemDao
import com.fridge.manager.core.database.ItemEntity
import com.fridge.manager.core.database.LocationDao
import com.fridge.manager.core.database.LocationEntity
import com.fridge.manager.core.media.CompressedItemPhotos
import com.fridge.manager.core.network.ApiErrorEnvelope
import com.fridge.manager.core.network.ApiService
import com.fridge.manager.core.network.ConsumeItemRequest
import com.fridge.manager.core.network.RefreshRequest
import com.fridge.manager.core.network.CreateHouseholdRequest
import com.fridge.manager.core.network.CreateItemRequest
import com.fridge.manager.core.network.CreateLocationRequest
import com.fridge.manager.core.network.DevLoginRequest
import com.fridge.manager.core.network.HealthResponseDto
import com.fridge.manager.core.network.ItemDto
import com.fridge.manager.core.network.JoinHouseholdRequest
import com.fridge.manager.core.network.LocationNodeDto
import com.fridge.manager.core.network.LoginRequest
import com.fridge.manager.core.network.PhotoUploadUrlRequest
import com.fridge.manager.core.network.RegisterRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.URI
import com.fridge.manager.core.network.UpdateItemRequest
import com.fridge.manager.core.database.AppDatabase
import com.fridge.manager.core.datastore.TokenStorage
import com.fridge.manager.domain.model.ActivityLogItem
import com.fridge.manager.domain.model.FoodItem
import com.fridge.manager.domain.model.HouseholdInfo
import com.fridge.manager.domain.model.StorageLocationNode
import com.fridge.manager.domain.usecase.BuildLocationTreeUseCase
import com.fridge.manager.domain.usecase.CalculateDDayUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

class ServerUnavailableException(val userMessage: String) : Exception(userMessage)
class AuthApiException(userMessage: String) : Exception(userMessage)

@Singleton
class FridgeRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val appDatabase: AppDatabase,
    private val tokenStorage: TokenStorage,
    private val buildLocationTreeUseCase: BuildLocationTreeUseCase,
    private val calculateDDayUseCase: CalculateDDayUseCase,
) {
    suspend fun checkServerHealth(): HealthResponseDto {
        val health = withTimeout(4_000L) {
            apiService.getHealth()
        }
        if (health.status != "healthy") {
            throw ServerUnavailableException("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.")
        }
        if (health.maintenance) {
            throw ServerUnavailableException(
                health.maintenanceMessage?.takeIf { it.isNotBlank() }
                    ?: "현재 서버 점검 중입니다. 잠시 후 다시 이용해 주세요.",
            )
        }
        if (BuildConfig.VERSION_CODE < health.minAndroidVersionCode) {
            throw ServerUnavailableException("최신 버전의 앱으로 업데이트가 필요합니다.")
        }
        return health
    }

    /** 서버 Refresh Token 폐기(실패해도 무시) 후 토큰·Room 캐시를 일괄 삭제한다. */
    suspend fun logout() {
        val refreshToken = tokenStorage.getRefreshToken()
        if (!refreshToken.isNullOrBlank()) {
            runCatching { withTimeout(4_000L) { apiService.logout(RefreshRequest(refreshToken)) } }
        }
        tokenStorage.clear()
        withContext(Dispatchers.IO) { appDatabase.clearAllTables() }
    }

    suspend fun devLogin(nickname: String): List<HouseholdInfo> {
        val tokens = apiService.devLogin(DevLoginRequest(nickname = nickname))
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
    }

    suspend fun login(username: String, password: String): List<HouseholdInfo> {
        val tokens = executeAuthCall {
            apiService.login(LoginRequest(username = username.trim(), password = password))
        }
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
    }

    suspend fun register(
        username: String,
        password: String,
        nickname: String,
        registrationCode: String,
    ): List<HouseholdInfo> {
        val tokens = executeAuthCall {
            apiService.register(
                RegisterRequest(
                    username = username.trim(),
                    password = password,
                    nickname = nickname.trim(),
                    registrationCode = registrationCode.trim(),
                ),
            )
        }
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
    }

    private suspend fun <T> executeAuthCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (err: HttpException) {
            throw AuthApiException(extractErrorMessage(err) ?: "인증에 실패했습니다. 입력 내용을 확인해 주세요.")
        }
    }

    private fun extractErrorMessage(err: HttpException): String? {
        val raw = runCatching { err.response()?.errorBody()?.string() }.getOrNull() ?: return null
        return runCatching {
            errorJson.decodeFromString<ApiErrorEnvelope>(raw).error?.message
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private companion object {
        val errorJson = Json { ignoreUnknownKeys = true }
    }

    suspend fun fetchAndSelectHousehold(): List<HouseholdInfo> {
        val me = apiService.getMe()
        val households = me.households.map { HouseholdInfo(it.id, it.name, it.role) }
        val current = tokenStorage.getCurrentHouseholdId()
        if (current == null || households.none { it.id == current }) {
            households.firstOrNull()?.let { tokenStorage.saveCurrentHouseholdId(it.id) }
        }
        return households
    }

    suspend fun createHousehold(name: String): HouseholdInfo {
        val dto = apiService.createHousehold(CreateHouseholdRequest(name = name))
        tokenStorage.saveCurrentHouseholdId(dto.id)
        return HouseholdInfo(dto.id, dto.name, dto.role)
    }

    suspend fun joinHousehold(code: String): HouseholdInfo {
        val dto = apiService.joinHousehold(JoinHouseholdRequest(code = code))
        tokenStorage.saveCurrentHouseholdId(dto.id)
        return HouseholdInfo(dto.id, dto.name, dto.role)
    }

    fun observeLocationTree(householdId: Long): Flow<List<StorageLocationNode>> {
        return locationDao.observeByHousehold(householdId).map { entities ->
            val flat = entities.map {
                StorageLocationNode(
                    id = it.id,
                    householdId = it.householdId,
                    parentId = it.parentId,
                    name = it.name,
                    storageType = it.storageType,
                    icon = it.icon,
                    sortOrder = it.sortOrder,
                    depth = it.depth,
                    fullPath = it.fullPath,
                )
            }
            buildLocationTreeUseCase(flat)
        }
    }

    fun observeFlatLocations(householdId: Long): Flow<List<StorageLocationNode>> {
        return locationDao.observeByHousehold(householdId).map { entities ->
            entities.map {
                StorageLocationNode(
                    id = it.id,
                    householdId = it.householdId,
                    parentId = it.parentId,
                    name = it.name,
                    storageType = it.storageType,
                    icon = it.icon,
                    sortOrder = it.sortOrder,
                    depth = it.depth,
                    fullPath = it.fullPath,
                )
            }
        }
    }

    suspend fun syncLocations(householdId: Long) {
        val remote = apiService.getLocations(householdId, format = "flat")
        locationDao.replaceHouseholdLocations(householdId, remote.map { it.toEntity() })
    }

    suspend fun createLocation(householdId: Long, parentId: Long?, name: String) {
        apiService.createLocation(householdId, CreateLocationRequest(parentId = parentId, name = name))
        syncLocations(householdId)
    }

    suspend fun deleteLocation(householdId: Long, locationId: Long) {
        apiService.deleteLocation(householdId, locationId, strategy = "cascade")
        syncLocations(householdId)
        syncItems(householdId)
    }

    fun observeActiveItems(householdId: Long, alertDays: Int = 3): Flow<List<FoodItem>> {
        return itemDao.observeItems(householdId, status = "ACTIVE").map { entities ->
            entities.map { entity ->
                val (dDay, state) = calculateDDayUseCase(entity.expiryDate, alertDays)
                FoodItem(
                    id = entity.id,
                    householdId = entity.householdId,
                    locationId = entity.locationId,
                    locationPath = entity.locationPath,
                    name = entity.name,
                    category = entity.category,
                    quantity = entity.quantity,
                    initialQuantity = entity.initialQuantity,
                    unit = entity.unit,
                    expiryDate = entity.expiryDate,
                    expiryType = entity.expiryType,
                    dDay = dDay,
                    expiryState = state,
                    status = entity.status,
                    memo = entity.memo,
                    detailImageUrl = resolveMediaUrl(entity.detailImageUrl),
                    thumbImageUrl = resolveMediaUrl(entity.thumbImageUrl),
                )
            }
        }
    }

    suspend fun syncItems(householdId: Long) {
        val remote = apiService.getItems(householdId, status = "ACTIVE")
        itemDao.replaceHouseholdItems(householdId, "ACTIVE", remote.map { it.toEntity() })
    }

    suspend fun uploadItemPhotos(
        householdId: Long,
        photos: CompressedItemPhotos,
    ): Pair<String, String> {
        val uploadUrls = apiService.createPhotoUploadUrl(
            householdId = householdId,
            body = PhotoUploadUrlRequest(
                detailSizeBytes = photos.detailBytes.size,
                thumbSizeBytes = photos.thumbBytes.size,
            ),
        )
        val webpType = "image/webp".toMediaType()
        apiService.uploadMediaBytes(
            uploadUrl = resolveMediaUrl(uploadUrls.detailUploadUrl) ?: uploadUrls.detailUploadUrl,
            body = photos.detailBytes.toRequestBody(webpType),
        )
        apiService.uploadMediaBytes(
            uploadUrl = resolveMediaUrl(uploadUrls.thumbUploadUrl) ?: uploadUrls.thumbUploadUrl,
            body = photos.thumbBytes.toRequestBody(webpType),
        )
        return uploadUrls.detailImageKey to uploadUrls.thumbImageKey
    }

    suspend fun createItem(householdId: Long, request: CreateItemRequest) {
        val created = apiService.createItem(householdId, request)
        itemDao.upsert(created.toEntity())
    }

    suspend fun updateQuantity(householdId: Long, itemId: Long, newQuantity: Double) {
        val updated = apiService.updateItem(householdId, itemId, UpdateItemRequest(quantity = newQuantity))
        itemDao.upsert(updated.toEntity())
    }

    suspend fun moveItem(householdId: Long, itemId: Long, newLocationId: Long) {
        val updated = apiService.updateItem(householdId, itemId, UpdateItemRequest(locationId = newLocationId))
        itemDao.upsert(updated.toEntity())
    }

    suspend fun consumeItem(
        householdId: Long,
        itemId: Long,
        amount: Double? = null,
        remainingQuantity: Double? = null,
        note: String? = null,
    ) {
        val updated = apiService.consumeItem(
            householdId,
            itemId,
            ConsumeItemRequest(
                amount = amount,
                remainingQuantity = remainingQuantity,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        itemDao.upsert(updated.toEntity())
    }

    suspend fun discardItem(householdId: Long, itemId: Long) {
        val updated = apiService.discardItem(householdId, itemId)
        itemDao.upsert(updated.toEntity())
    }

    suspend fun createInvite(householdId: Long): String {
        return apiService.createInvite(householdId).code
    }

    suspend fun getActivities(householdId: Long): List<ActivityLogItem> {
        return apiService.getActivities(householdId).items.map {
            val detailMap = it.detail
            val baseName = detailMap?.get("name")?.jsonPrimitive?.contentOrNull
                ?: detailMap?.get("nickname")?.jsonPrimitive?.contentOrNull
            val summary = if (it.actionType.uppercase() == "CONSUMED" && baseName != null && detailMap != null) {
                val consumed = detailMap["consumed"]?.jsonPrimitive?.doubleOrNull
                val remaining = detailMap["remaining"]?.jsonPrimitive?.doubleOrNull
                val unit = detailMap["unit"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val note = detailMap["note"]?.jsonPrimitive?.contentOrNull
                val amountText = when {
                    consumed != null && remaining != null && remaining > 0.0 ->
                        "$baseName ${formatQty(consumed)}$unit 소비 (남음: ${formatQty(remaining)}$unit)"
                    consumed != null && remaining == 0.0 ->
                        "$baseName ${formatQty(consumed)}$unit 전량 소비"
                    else -> baseName
                }
                if (!note.isNullOrBlank()) "$amountText · $note" else amountText
            } else {
                baseName
            }
            ActivityLogItem(
                id = it.id,
                actorNickname = it.actorNickname,
                entityType = it.entityType,
                actionType = it.actionType,
                summaryName = summary,
                createdAt = it.createdAt,
            )
        }
    }

    private fun formatQty(value: Double): String {
        val rounded = kotlin.math.round(value * 100.0) / 100.0
        return if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }

    private fun LocationNodeDto.toEntity() = LocationEntity(
        id = id,
        householdId = householdId,
        parentId = parentId,
        name = name,
        storageType = storageType,
        icon = icon,
        sortOrder = sortOrder,
        depth = depth,
        fullPath = fullPath,
    )

    private fun ItemDto.toEntity() = ItemEntity(
        id = id,
        householdId = householdId,
        locationId = locationId,
        locationPath = locationPath,
        name = name,
        category = category,
        quantity = quantity,
        initialQuantity = initialQuantity ?: quantity,
        unit = unit,
        expiryDate = expiryDate,
        expiryType = expiryType,
        status = status,
        memo = memo,
        detailImageKey = detailImageKey,
        thumbImageKey = thumbImageKey,
        detailImageUrl = detailImageUrl,
        thumbImageUrl = thumbImageUrl,
        updatedAt = updatedAt,
    )

    private fun resolveMediaUrl(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) return rawUrl
        val baseUri = URI(BuildConfig.API_BASE_URL)
        val origin = buildString {
            append(baseUri.scheme)
            append("://")
            append(baseUri.host)
            if (baseUri.port != -1) {
                append(":")
                append(baseUri.port)
            }
        }
        return if (rawUrl.startsWith("/")) "$origin$rawUrl" else "$origin/$rawUrl"
    }
}
