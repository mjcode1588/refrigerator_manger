package com.fridge.manager.domain.usecase

import com.fridge.manager.domain.model.ExpiryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculateDDayUseCaseTest {

    private val useCase = CalculateDDayUseCase()
    private val today = LocalDate.of(2026, 11, 20)

    @Test
    fun `null or blank expiry returns NONE`() {
        val (dDay, state) = useCase(null, alertDays = 3, today = today)
        assertNull(dDay)
        assertEquals(ExpiryState.NONE, state)
    }

    @Test
    fun `past expiry returns EXPIRED with negative dDay`() {
        val (dDay, state) = useCase("2026-11-18", alertDays = 3, today = today)
        assertEquals(-2, dDay)
        assertEquals(ExpiryState.EXPIRED, state)
    }

    @Test
    fun `same day expiry returns TODAY`() {
        val (dDay, state) = useCase("2026-11-20", alertDays = 3, today = today)
        assertEquals(0, dDay)
        assertEquals(ExpiryState.TODAY, state)
    }

    @Test
    fun `within alertDays returns EXPIRING_SOON`() {
        val (dDay, state) = useCase("2026-11-23", alertDays = 3, today = today)
        assertEquals(3, dDay)
        assertEquals(ExpiryState.EXPIRING_SOON, state)
    }

    @Test
    fun `beyond alertDays returns SAFE`() {
        val (dDay, state) = useCase("2026-11-30", alertDays = 3, today = today)
        assertEquals(10, dDay)
        assertEquals(ExpiryState.SAFE, state)
    }
}
