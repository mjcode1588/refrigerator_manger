package com.fridge.manager.domain.usecase

import com.fridge.manager.domain.model.StorageLocationNode
import javax.inject.Inject

class BuildLocationTreeUseCase @Inject constructor() {

    operator fun invoke(flatNodes: List<StorageLocationNode>): List<StorageLocationNode> {
        val byParent = flatNodes
            .sortedWith(compareBy<StorageLocationNode> { it.sortOrder }.thenBy { it.id })
            .groupBy { it.parentId }

        fun build(parentId: Long?): List<StorageLocationNode> {
            return byParent[parentId].orEmpty().map { node ->
                node.copy(children = build(node.id))
            }
        }
        return build(null)
    }
}
