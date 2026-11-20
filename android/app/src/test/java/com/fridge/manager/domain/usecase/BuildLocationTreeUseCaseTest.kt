package com.fridge.manager.domain.usecase

import com.fridge.manager.domain.model.StorageLocationNode
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildLocationTreeUseCaseTest {

    private val useCase = BuildLocationTreeUseCase()

    @Test
    fun `builds multi-level tree ordered by sortOrder and id`() {
        val flat = listOf(
            StorageLocationNode(3, 1, 1, "냉동실", "FREEZER", null, 1, 1, "냉장고 > 냉동실"),
            StorageLocationNode(1, 1, null, "냉장고", "FRIDGE", null, 0, 0, "냉장고"),
            StorageLocationNode(2, 1, 1, "냉장실", "FRIDGE", null, 0, 1, "냉장고 > 냉장실"),
            StorageLocationNode(4, 1, 2, "문쪽 선반", "FRIDGE", null, 0, 2, "냉장고 > 냉장실 > 문쪽 선반"),
        )

        val tree = useCase(flat)
        assertEquals(1, tree.size)
        val fridge = tree.first()
        assertEquals(listOf("냉장실", "냉동실"), fridge.children.map { it.name })
        assertEquals(listOf("문쪽 선반"), fridge.children.first().children.map { it.name })
    }
}
