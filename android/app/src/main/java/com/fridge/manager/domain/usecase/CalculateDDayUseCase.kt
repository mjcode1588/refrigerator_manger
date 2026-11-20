package com.fridge.manager.domain.usecase

import com.fridge.manager.domain.model.ExpiryState
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class CalculateDDayUseCase @Inject constructor() {

    operator fun invoke(
        expiryDateIso: String?,
        alertDays: Int = 3,
        today: LocalDate = LocalDate.now(KST_ZONE),
    ): Pair<Int?, ExpiryState> {
        if (expiryDateIso.isNullOrBlank()) return null to ExpiryState.NONE

        val expiry = LocalDate.parse(expiryDateIso)
        val dDay = ChronoUnit.DAYS.between(today, expiry).toInt()
        val state = when {
            dDay < 0 -> ExpiryState.EXPIRED
            dDay == 0 -> ExpiryState.TODAY
            dDay <= alertDays -> ExpiryState.EXPIRING_SOON
            else -> ExpiryState.SAFE
        }
        return dDay to state
    }

    companion object {
        val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
