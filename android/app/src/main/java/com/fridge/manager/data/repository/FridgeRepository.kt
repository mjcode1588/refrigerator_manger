package com.fridge.manager.data.repository

import com.fridge.manager.core.database.ItemDao
import com.fridge.manager.core.database.ItemEntity
import com.fridge.manager.core.database.LocationDao
import com.fridge.manager.core.database.LocationEntity
import com.fridge.manager.core.network.ApiService
import com.fridge.manager.core.network.ConsumeItemRequest
import com.fridge.manager.core.network.CreateHouseholdRequest
import com.fridge.manager.core.network.CreateItemRequest
import com.fridge.manager.core.network.CreateLocationRequest
import com.fridge.manager.core.network.DevLoginRequest
import com.fridge.manager.core.network.ItemDto
import com.fridge.manager.core.network.JoinHouseholdRequest
import com.fridge.manager.core.network.LocationNodeDto
import com.fridge.manager.core.network.SocialGoogleRequest
import com.fridge.manager.core.network.SocialKakaoRequest
import com.fridge.manager.core.network.UpdateItemRequest
import com.fridge.manager.core.datastore.TokenStorage
import com.fridge.manager.domain.model.ActivityLogItem
import com.fridge.manager.domain.model.FoodItem
import com.fridge.manager.domain.model.HouseholdInfo
import com.fridge.manager.domain.model.StorageLocationNode
import com.fridge.manager.domain.usecase.BuildLocationTreeUseCase
import com.fridge.manager.domain.usecase.CalculateDDayUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FridgeRepository @Inject constructor(
    private val apiService: ApiService,
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val tokenStorage: TokenStorage,
    private val buildLocationTreeUseCase: BuildLocationTreeUseCase,
    private val calculateDDayUseCase: CalculateDDayUseCase,
) {
    suspend fun devLogin(nickname: String): List<HouseholdInfo> {
        val tokens = apiService.devLogin(DevLoginRequest(nickname = nickname))
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
    }

    suspend fun loginWithGoogle(idToken: String): List<HouseholdInfo> {
        val tokens = apiService.loginWithGoogle(SocialGoogleRequest(idToken))
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
    }

    suspend fun loginWithKakao(accessToken: String): List<HouseholdInfo> {
        val tokens = apiService.loginWithKakao(SocialKakaoRequest(accessToken))
        tokenStorage.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.user.nickname)
        return fetchAndSelectHousehold()
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
                    unit = entity.unit,
                    expiryDate = entity.expiryDate,
                    expiryType = entity.expiryType,
                    dDay = dDay,
                    expiryState = state,
                    status = entity.status,
                    memo = entity.memo,
                )
            }
        }
    }

    suspend fun syncItems(householdId: Long) {
        val remote = apiService.getItems(householdId, status = "ACTIVE")
        itemDao.replaceHouseholdItems(householdId, "ACTIVE", remote.map { it.toEntity() })
    }

    suspend fun createItem(householdId: Long, request: CreateItemRequest) {
        val created = apiService.createItem(householdId, request)
        itemDao.upsert(created.toEntity())
    }

    suspend fun updateQuantity(householdId: Long, itemId: Long, newQuantity: Double) {
        val updated = apiService.updateItem(householdId, itemId, UpdateItemRequest(quantity = newQuantity))
        itemDao.upsert(updated.toEntity())
    }

    suspend fun consumeItem(householdId: Long, itemId: Long, amount: Double? = null) {
        val updated = apiService.consumeItem(householdId, itemId, amount?.let { ConsumeItemRequest(it) })
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
            ActivityLogItem(
                id = it.id,
                actorNickname = it.actorNickname,
                entityType = it.entityType,
                actionType = it.actionType,
                summaryName = it.detail?.get("name") ?: it.detail?.get("nickname"),
                createdAt = it.createdAt,
            )
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
        unit = unit,
        expiryDate = expiryDate,
        expiryType = expiryType,
        status = status,
        memo = memo,
        updatedAt = updatedAt,
    )
}
