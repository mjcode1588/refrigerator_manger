package com.fridge.manager.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fridge.manager.core.datastore.NotificationPreferences
import com.fridge.manager.core.datastore.NotificationSettings
import com.fridge.manager.core.datastore.TokenStorage
import com.fridge.manager.core.media.CompressedItemPhotos
import com.fridge.manager.core.network.CreateItemRequest
import com.fridge.manager.core.notification.ExpiryCheckWorker
import com.fridge.manager.data.repository.FridgeRepository
import com.fridge.manager.data.repository.ServerUnavailableException
import com.fridge.manager.domain.model.ActivityLogItem
import com.fridge.manager.domain.model.FoodItem
import com.fridge.manager.domain.model.HouseholdInfo
import com.fridge.manager.domain.model.StorageLocationNode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: FridgeRepository,
    private val tokenStorage: TokenStorage,
    private val notificationPreferences: NotificationPreferences,
) : ViewModel() {

    val isLoggedIn: StateFlow<String?> = tokenStorage.accessTokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val currentHouseholdId: StateFlow<Long?> = tokenStorage.currentHouseholdIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val nickname: StateFlow<String?> = tokenStorage.nicknameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val notificationSettings: StateFlow<NotificationSettings> = notificationPreferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotificationSettings())

    val locationTree: StateFlow<List<StorageLocationNode>> = currentHouseholdId
        .filterNotNull()
        .flatMapLatest { repository.observeLocationTree(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val flatLocations: StateFlow<List<StorageLocationNode>> = currentHouseholdId
        .filterNotNull()
        .flatMapLatest { repository.observeFlatLocations(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeItems: StateFlow<List<FoodItem>> = currentHouseholdId
        .filterNotNull()
        .flatMapLatest { repository.observeActiveItems(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _households = MutableStateFlow<List<HouseholdInfo>>(emptyList())
    val households: StateFlow<List<HouseholdInfo>> = _households.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityLogItem>>(emptyList())
    val activities: StateFlow<List<ActivityLogItem>> = _activities.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoginLoading = MutableStateFlow(false)
    val isLoginLoading: StateFlow<Boolean> = _isLoginLoading.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun devLogin(nickname: String) = viewModelScope.launch {
        if (_isLoginLoading.value) return@launch
        _isLoginLoading.value = true
        try {
            runCatching {
                repository.checkServerHealth()
            }.onFailure { err ->
                _message.value = if (err is ServerUnavailableException) {
                    err.userMessage
                } else {
                    "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
                }
                return@launch
            }

            runCatching {
                val list = repository.devLogin(nickname)
                _households.value = list
                refreshAll()
            }.onFailure { _message.value = it.message ?: "로그인 실패" }
        } finally {
            _isLoginLoading.value = false
        }
    }

    fun createHousehold(name: String) = viewModelScope.launch {
        runCatching {
            repository.createHousehold(name)
            _households.value = repository.fetchAndSelectHousehold()
            refreshAll()
        }.onFailure { _message.value = it.message ?: "가구 생성 실패" }
    }

    fun joinHousehold(code: String) = viewModelScope.launch {
        runCatching {
            repository.joinHousehold(code)
            _households.value = repository.fetchAndSelectHousehold()
            refreshAll()
        }.onFailure { _message.value = it.message ?: "초대 참여 실패" }
    }

    fun refreshAll() = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            if (_households.value.isEmpty()) {
                _households.value = repository.fetchAndSelectHousehold()
            }
            repository.syncLocations(hhId)
            repository.syncItems(hhId)
            _activities.value = repository.getActivities(hhId)
        }.onFailure { _message.value = "동기화 실패 (오프라인 캐시 표시 중)" }
    }

    fun switchHousehold(householdId: Long) = viewModelScope.launch {
        tokenStorage.saveCurrentHouseholdId(householdId)
        _inviteCode.value = null
        refreshAll()
    }

    fun createLocation(parentId: Long?, name: String) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            repository.createLocation(hhId, parentId, name)
            _activities.value = repository.getActivities(hhId)
        }.onFailure { _message.value = it.message }
    }

    fun deleteLocation(locationId: Long) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            repository.deleteLocation(hhId, locationId)
            _activities.value = repository.getActivities(hhId)
        }.onFailure { _message.value = it.message }
    }

    fun createItem(
        request: CreateItemRequest,
        photos: CompressedItemPhotos? = null,
    ) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            val finalRequest = if (photos != null) {
                val (detailKey, thumbKey) = repository.uploadItemPhotos(hhId, photos)
                request.copy(detailImageKey = detailKey, thumbImageKey = thumbKey)
            } else {
                request
            }
            repository.createItem(hhId, finalRequest)
            _activities.value = repository.getActivities(hhId)
        }.onFailure { _message.value = it.message }
    }

    fun adjustQuantity(item: FoodItem, delta: Double) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            val next = item.quantity + delta
            if (next <= 0.0) {
                repository.consumeItem(hhId, item.id)
            } else {
                repository.updateQuantity(hhId, item.id, next)
            }
            _activities.value = repository.getActivities(hhId)
        }.onFailure { _message.value = it.message }
    }

    fun consumeItem(
        itemId: Long,
        amount: Double? = null,
        remainingQuantity: Double? = null,
        note: String? = null,
    ) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            repository.consumeItem(
                householdId = hhId,
                itemId = itemId,
                amount = amount,
                remainingQuantity = remainingQuantity,
                note = note,
            )
            _activities.value = repository.getActivities(hhId)
            val isPartial = (remainingQuantity != null && remainingQuantity > 0.0) || amount != null
            _message.value = if (isPartial) {
                "먹고 남은 양을 기록했습니다."
            } else {
                "소비 완료 처리했습니다."
            }
        }.onFailure { _message.value = it.message }
    }

    fun discardItem(itemId: Long) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            repository.discardItem(hhId, itemId)
            _activities.value = repository.getActivities(hhId)
            _message.value = "폐기 처리했습니다."
        }.onFailure { _message.value = it.message }
    }

    fun moveItem(itemId: Long, newLocationId: Long) = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            repository.moveItem(hhId, itemId, newLocationId)
            _activities.value = repository.getActivities(hhId)
            _message.value = "보관 장소를 이동했습니다."
        }.onFailure { _message.value = it.message }
    }

    fun generateInviteCode() = viewModelScope.launch {
        val hhId = tokenStorage.getCurrentHouseholdId() ?: return@launch
        runCatching {
            _inviteCode.value = repository.createInvite(hhId)
        }.onFailure { _message.value = it.message }
    }

    fun updateNotificationSettings(newSettings: NotificationSettings) = viewModelScope.launch {
        notificationPreferences.updateSettings(newSettings)
        if (newSettings.enabled) {
            ExpiryCheckWorker.scheduleDaily(appContext, newSettings.hour, newSettings.minute)
        } else {
            ExpiryCheckWorker.cancel(appContext)
        }
        _message.value = "알림 설정을 저장했습니다."
    }

    fun logout() = viewModelScope.launch {
        repository.logout()
        ExpiryCheckWorker.cancel(appContext)
        _households.value = emptyList()
        _activities.value = emptyList()
    }
}
