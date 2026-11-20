package com.fridge.manager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fridge.manager.core.network.CreateItemRequest
import com.fridge.manager.domain.model.ExpiryState
import com.fridge.manager.domain.model.FoodItem
import com.fridge.manager.domain.model.StorageLocationNode

@Composable
fun InventoryScreen(
    locationsTree: List<StorageLocationNode>,
    flatLocations: List<StorageLocationNode>,
    items: List<FoodItem>,
    expiringOnlyInitial: Boolean,
    onCreateLocation: (parentId: Long?, name: String) -> Unit,
    onDeleteLocation: (locationId: Long) -> Unit,
    onCreateItem: (CreateItemRequest) -> Unit,
    onAdjustQuantity: (FoodItem, Double) -> Unit,
    onConsumeItem: (Long) -> Unit,
    onDiscardItem: (Long) -> Unit,
) {
    var selectedLocationId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expiringOnly by remember(expiringOnlyInitial) { mutableStateOf(expiringOnlyInitial) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddLocationParentId by remember { mutableStateOf<Long?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }

    val filteredItems = items.filter { item ->
        val matchesQuery = searchQuery.isBlank() ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            (item.memo?.contains(searchQuery, ignoreCase = true) == true)
        val matchesExpiring = !expiringOnly || item.expiryState in listOf(
            ExpiryState.EXPIRED,
            ExpiryState.TODAY,
            ExpiryState.EXPIRING_SOON,
        )
        val matchesLoc = selectedLocationId == null || item.locationId == selectedLocationId
        matchesQuery && matchesExpiring && matchesLoc
    }

    Scaffold(
        floatingActionButton = {
            if (flatLocations.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddItemDialog = true }) {
                    Text("+ 품목")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📂 보관 장소 (자유 트리)", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        showAddLocationParentId = null
                        showAddLocationDialog = true
                    }) {
                        Text("+ 최상위 장소")
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLocationId == null,
                        onClick = { selectedLocationId = null },
                        label = { Text("전체") },
                    )
                    FilterChip(
                        selected = expiringOnly,
                        onClick = { expiringOnly = !expiringOnly },
                        label = { Text("임박/만료만") },
                    )
                }
            }

            items(flatLocations, key = { "loc-${it.id}" }) { loc ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLocationId = loc.id }
                        .padding(start = (loc.depth * 14).dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${loc.icon ?: "📁"} ${loc.name}",
                        color = if (selectedLocationId == loc.id) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Row {
                        TextButton(onClick = {
                            showAddLocationParentId = loc.id
                            showAddLocationDialog = true
                        }) { Text("+하위") }
                        TextButton(onClick = { onDeleteLocation(loc.id) }) { Text("삭제") }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("품목 검색") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            items(filteredItems, key = { "item-${it.id}" }) { item ->
                ItemCard(
                    item = item,
                    onAdjustQuantity = onAdjustQuantity,
                    onConsumeItem = onConsumeItem,
                    onDiscardItem = onDiscardItem,
                )
            }
        }
    }

    if (showAddLocationDialog) {
        var locName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("보관 장소 추가") },
            text = {
                OutlinedTextField(
                    value = locName,
                    onValueChange = { locName = it },
                    label = { Text("장소 이름") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (locName.isNotBlank()) {
                        onCreateLocation(showAddLocationParentId, locName.trim())
                        showAddLocationDialog = false
                    }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) { Text("취소") }
            },
        )
    }

    if (showAddItemDialog && flatLocations.isNotEmpty()) {
        var name by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var unit by remember { mutableStateOf("개") }
        var expiry by remember { mutableStateOf("") }
        val targetLocId = selectedLocationId ?: flatLocations.first().id

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("품목 등록") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("품목명") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("수량") },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("단위") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { expiry = it },
                        label = { Text("유통기한 (YYYY-MM-DD, 선택)") },
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsedQty = qty.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        onCreateItem(
                            CreateItemRequest(
                                locationId = targetLocId,
                                name = name.trim(),
                                quantity = parsedQty,
                                unit = unit.ifBlank { "개" },
                                expiryDate = expiry.trim().ifBlank { null },
                            )
                        )
                        showAddItemDialog = false
                    }
                }) { Text("등록") }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) { Text("취소") }
            },
        )
    }
}

@Composable
private fun ItemCard(
    item: FoodItem,
    onAdjustQuantity: (FoodItem, Double) -> Unit,
    onConsumeItem: (Long) -> Unit,
    onDiscardItem: (Long) -> Unit,
) {
    val dDayLabel = when (item.expiryState) {
        ExpiryState.NONE -> "기한 없음"
        ExpiryState.EXPIRED -> "D+${kotlin.math.abs(item.dDay ?: 0)} 만료"
        ExpiryState.TODAY -> "D-Day"
        else -> "D-${item.dDay}"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(item.locationPath, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = {}, label = { Text(dDayLabel) })
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onAdjustQuantity(item, -1.0) }) { Text("-") }
                    Text(
                        "${item.quantity} ${item.unit}",
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    OutlinedButton(onClick = { onAdjustQuantity(item, 1.0) }) { Text("+") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { onConsumeItem(item.id) }) { Text("소비") }
                    OutlinedButton(onClick = { onDiscardItem(item.id) }) { Text("폐기") }
                }
            }
        }
    }
}
