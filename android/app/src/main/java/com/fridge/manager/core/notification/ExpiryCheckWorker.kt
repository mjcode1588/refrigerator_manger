package com.fridge.manager.core.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fridge.manager.core.database.ItemDao
import com.fridge.manager.core.database.ItemEntity
import com.fridge.manager.core.datastore.NotificationPreferences
import com.fridge.manager.core.datastore.TokenStorage
import com.fridge.manager.core.network.ApiService
import com.fridge.manager.domain.model.ExpiryState
import com.fridge.manager.domain.usecase.CalculateDDayUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@HiltWorker
class ExpiryCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val apiService: ApiService,
    private val itemDao: ItemDao,
    private val tokenStorage: TokenStorage,
    private val notificationPreferences: NotificationPreferences,
    private val notificationHelper: NotificationHelper,
    private val calculateDDayUseCase: CalculateDDayUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = notificationPreferences.getSettings()
        if (!settings.enabled) return Result.success()

        val householdId = tokenStorage.getCurrentHouseholdId() ?: return Result.success()
        val today = LocalDate.now(CalculateDDayUseCase.KST_ZONE)
        val todayIso = today.toString()

        if (notificationPreferences.getLastNotifiedDate() == todayIso) {
            return Result.success()
        }

        // 네트워크 동기화 시도 후 실패 시 Room 로컬 캐시 폴백
        runCatching {
            val remote = apiService.getExpiringItems(householdId, withinDays = settings.alertDays)
            val entities = remote.map { dto ->
                ItemEntity(
                    id = dto.id,
                    householdId = dto.householdId,
                    locationId = dto.locationId,
                    locationPath = dto.locationPath,
                    name = dto.name,
                    category = dto.category,
                    quantity = dto.quantity,
                    initialQuantity = dto.initialQuantity ?: dto.quantity,
                    unit = dto.unit,
                    expiryDate = dto.expiryDate,
                    expiryType = dto.expiryType,
                    status = dto.status,
                    memo = dto.memo,
                    detailImageKey = dto.detailImageKey,
                    thumbImageKey = dto.thumbImageKey,
                    detailImageUrl = dto.detailImageUrl,
                    thumbImageUrl = dto.thumbImageUrl,
                    updatedAt = dto.updatedAt,
                )
            }
            itemDao.insertAll(entities)
        }

        val cutoffIso = today.plusDays(settings.alertDays.toLong()).toString()
        val candidates = itemDao.getExpiringItems(householdId, cutoffIso)
        val alertItems = candidates.filter { item ->
            val (_, state) = calculateDDayUseCase(item.expiryDate, settings.alertDays, today)
            when (state) {
                ExpiryState.EXPIRED -> settings.includeExpired
                ExpiryState.TODAY, ExpiryState.EXPIRING_SOON -> true
                else -> false
            }
        }

        if (alertItems.isNotEmpty()) {
            val first = alertItems.first()
            val title = if (alertItems.size == 1) {
                "⏰ 유통기한 임박: ${first.name}"
            } else {
                "⏰ 유통기한 확인 필요 (${alertItems.size}건)"
            }
            val body = if (alertItems.size == 1) {
                "${first.locationPath} · 기한 ${first.expiryDate}"
            } else {
                "${first.name} 외 ${alertItems.size - 1}개 품목의 기한을 확인하세요."
            }
            notificationHelper.showExpirySummary(title, body)
            notificationPreferences.setLastNotifiedDate(todayIso)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "daily_expiry_check"

        fun scheduleDaily(context: Context, hour: Int, minute: Int) {
            val now = ZonedDateTime.now(CalculateDDayUseCase.KST_ZONE)
            var nextRun = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!nextRun.isAfter(now)) {
                nextRun = nextRun.plusDays(1)
            }
            val initialDelayMinutes = Duration.between(now, nextRun).toMinutes().coerceAtLeast(1)

            val request = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
