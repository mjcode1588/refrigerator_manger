package com.fridge.manager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fridge.manager.domain.model.ShoppingItem
import com.fridge.manager.ui.theme.FreshKitchenColors
import com.fridge.manager.ui.theme.FreshKitchenShapes
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

private data class ShoppingCategoryOption(
    val code: String,
    val label: String,
)

private val SHOPPING_CATEGORIES = listOf(
    ShoppingCategoryOption("FRESH", "채소/과일"),
    ShoppingCategoryOption("MEAT_FISH", "정육/수산"),
    ShoppingCategoryOption("DAIRY", "유제품"),
    ShoppingCategoryOption("PROCESSED", "가공식품"),
    ShoppingCategoryOption("BEVERAGE", "음료"),
    ShoppingCategoryOption("SAUCE", "양념/소스"),
    ShoppingCategoryOption("ETC", "기타"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    householdName: String,
    items: List<ShoppingItem>,
    onRefresh: () -> Unit,
    onTogglePurchased: (ShoppingItem) -> Unit,
    onCreateItem: (name: String, category: String, quantity: Double, unit: String, memo: String?) -> Unit,
    onUpdateItem: (itemId: Long, name: String, category: String, quantity: Double, unit: String, memo: String?) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onClearPurchased: () -> Unit,
) {
    val unpurchasedItems = remember(items) { items.filter { !it.isPurchased } }
    val purchasedItems = remember(items) { items.filter { it.isPurchased } }

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var confirmClearPurchased by remember { mutableStateOf(false) }
    var pendingDeleteItem by remember { mutableStateOf<ShoppingItem?>(null) }

    Scaffold(
        containerColor = FreshKitchenColors.CanvasBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showEditorSheet = true
                },
                containerColor = FreshKitchenColors.PrimaryTealDeep,
                contentColor = Color.White,
                shape = FreshKitchenShapes.Control,
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "장보기 품목 추가")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "장보기 리스트",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FreshKitchenColors.TextPrimary,
                    )
                    Text(
                        text = "$householdName · 살 것 ${unpurchasedItems.size}개",
                        style = MaterialTheme.typography.bodySmall,
                        color = FreshKitchenColors.TextMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (purchasedItems.isNotEmpty()) {
                        TextButton(onClick = { confirmClearPurchased = true }) {
                            Text(
                                text = "완료 비우기",
                                style = MaterialTheme.typography.labelLarge,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "새로고침",
                            tint = FreshKitchenColors.TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = FreshKitchenShapes.Card,
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(FreshKitchenColors.PrimarySoft),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ShoppingCart,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "장보기 리스트가 비어 있어요",
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.TextPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "사야 할 식재료를 등록하면 가족이 먼저 구매했을 때 표시되어 중복 구매를 막아줘요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextMuted,
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    editingItem = null
                                    showEditorSheet = true
                                },
                                shape = FreshKitchenShapes.Control,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FreshKitchenColors.PrimaryTealDeep,
                                ),
                            ) {
                                Text("품목 추가하기")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (unpurchasedItems.isNotEmpty()) {
                        items(unpurchasedItems, key = { it.id }) { item ->
                            ShoppingItemCard(
                                item = item,
                                modifier = Modifier.animateItem(),
                                onTogglePurchased = { onTogglePurchased(item) },
                                onEdit = {
                                    editingItem = item
                                    showEditorSheet = true
                                },
                                onDelete = { pendingDeleteItem = item },
                            )
                        }
                    }

                    if (purchasedItems.isNotEmpty()) {
                        item(key = "purchased_header") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                            ) {
                                if (unpurchasedItems.isNotEmpty()) {
                                    HorizontalDivider(color = FreshKitchenColors.BorderSubtle)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "구매 완료 (${purchasedItems.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = FreshKitchenColors.TextSecondary,
                                    )
                                    Text(
                                        text = "이미 구매됨 · 안 사도 돼요",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FreshKitchenColors.PrimaryTealDeep,
                                    )
                                }
                            }
                        }
                        items(purchasedItems, key = { it.id }) { item ->
                            ShoppingItemCard(
                                item = item,
                                modifier = Modifier.animateItem(),
                                onTogglePurchased = { onTogglePurchased(item) },
                                onEdit = {
                                    editingItem = item
                                    showEditorSheet = true
                                },
                                onDelete = { pendingDeleteItem = item },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
        ShoppingItemEditorSheet(
            initialItem = editingItem,
            onDismiss = { showEditorSheet = false },
            onSave = { name, category, quantity, unit, memo ->
                val current = editingItem
                if (current == null) {
                    onCreateItem(name, category, quantity, unit, memo)
                } else {
                    onUpdateItem(current.id, name, category, quantity, unit, memo)
                }
                showEditorSheet = false
            },
        )
    }

    if (confirmClearPurchased) {
        AlertDialog(
            onDismissRequest = { confirmClearPurchased = false },
            containerColor = FreshKitchenColors.SurfaceWhite,
            title = { Text("구매 완료 항목을 비울까요?") },
            text = {
                Text("구매 완료로 표시된 ${purchasedItems.size}개 항목이 리스트에서 삭제됩니다.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmClearPurchased = false
                        onClearPurchased()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshKitchenColors.PrimaryTealDeep,
                    ),
                ) {
                    Text("비우기")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearPurchased = false }) {
                    Text("취소")
                }
            },
        )
    }

    pendingDeleteItem?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            containerColor = FreshKitchenColors.SurfaceWhite,
            title = { Text("품목을 삭제할까요?") },
            text = { Text("'${target.name}' 항목이 장보기 리스트에서 삭제됩니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteItem = null
                        onDeleteItem(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshKitchenColors.ExpiredText,
                    ),
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    modifier: Modifier = Modifier,
    onTogglePurchased: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val purchased = item.isPurchased
    val containerColor = if (purchased) {
        FreshKitchenColors.SurfaceSubtle
    } else {
        FreshKitchenColors.SurfaceWhite
    }
    val categoryLabel = remember(item.category) {
        SHOPPING_CATEGORIES.find { it.code == item.category }?.label ?: "기타"
    }
    val formattedQty = remember(item.quantity, item.unit) {
        val rounded = round(item.quantity * 100.0) / 100.0
        val numText = if (rounded % 1.0 == 0.0) {
            rounded.toLong().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
        "$numText ${item.unit}"
    }
    val purchasedSummary = remember(item.purchasedByNickname, item.purchasedAt) {
        val who = item.purchasedByNickname?.takeIf { it.isNotBlank() }?.let { "${it}님 구매 완료" }
            ?: "구매 완료"
        val timeText = formatPurchasedTime(item.purchasedAt)
        if (timeText != null) "$who · $timeText" else who
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FreshKitchenShapes.Card,
        color = containerColor,
        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTogglePurchased)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onTogglePurchased,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (purchased) {
                        Icons.Rounded.CheckCircle
                    } else {
                        Icons.Outlined.RadioButtonUnchecked
                    },
                    contentDescription = if (purchased) "구매 완료 취소" else "구매 완료 표시",
                    tint = if (purchased) {
                        FreshKitchenColors.PrimaryTealDeep
                    } else {
                        FreshKitchenColors.TextMuted
                    },
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (purchased) {
                            FreshKitchenColors.TextMuted
                        } else {
                            FreshKitchenColors.TextPrimary
                        },
                        textDecoration = if (purchased) {
                            TextDecoration.LineThrough
                        } else {
                            TextDecoration.None
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Surface(
                        shape = FreshKitchenShapes.Pill,
                        color = FreshKitchenColors.SurfaceMuted,
                    ) {
                        Text(
                            text = formattedQty,
                            style = MaterialTheme.typography.labelMedium,
                            color = FreshKitchenColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = categoryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = FreshKitchenColors.TextMuted,
                    )
                    if (!item.memo.isNullOrBlank()) {
                        Text(
                            text = "· ${item.memo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = FreshKitchenColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (purchased) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = FreshKitchenShapes.Pill,
                        color = FreshKitchenColors.PrimarySoft,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = purchasedSummary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }
                }
            }

            if (!purchased) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "수정",
                        tint = FreshKitchenColors.TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "삭제",
                    tint = FreshKitchenColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShoppingItemEditorSheet(
    initialItem: ShoppingItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, quantity: Double, unit: String, memo: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initialItem) { mutableStateOf(initialItem?.name.orEmpty()) }
    var category by remember(initialItem) { mutableStateOf(initialItem?.category ?: "ETC") }
    var quantityText by remember(initialItem) {
        mutableStateOf(
            initialItem?.quantity?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: "1",
        )
    }
    var unit by remember(initialItem) { mutableStateOf(initialItem?.unit ?: "개") }
    var memo by remember(initialItem) { mutableStateOf(initialItem?.memo.orEmpty()) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = FreshKitchenColors.SurfaceMuted,
        unfocusedContainerColor = FreshKitchenColors.SurfaceMuted,
        focusedBorderColor = FreshKitchenColors.PrimaryTealDeep,
        unfocusedBorderColor = Color.Transparent,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = FreshKitchenColors.SurfaceWhite,
        shape = FreshKitchenShapes.Sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (initialItem == null) "장보기 품목 추가" else "장보기 품목 수정",
                style = MaterialTheme.typography.titleLarge,
                color = FreshKitchenColors.TextPrimary,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("품목명") },
                placeholder = { Text("예: 서울우유 1L, 계란 30구") },
                singleLine = true,
                shape = FreshKitchenShapes.Control,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("수량") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = FreshKitchenShapes.Control,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("단위") },
                    placeholder = { Text("개, 팩, g") },
                    singleLine = true,
                    shape = FreshKitchenShapes.Control,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = "카테고리",
                style = MaterialTheme.typography.labelLarge,
                color = FreshKitchenColors.TextSecondary,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SHOPPING_CATEGORIES.forEach { option ->
                    val selected = category == option.code
                    FilterChip(
                        selected = selected,
                        onClick = { category = option.code },
                        label = { Text(option.label) },
                        shape = FreshKitchenShapes.Pill,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FreshKitchenColors.PrimarySoft,
                            selectedLabelColor = FreshKitchenColors.PrimaryTealDeep,
                        ),
                    )
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모 (선택)") },
                placeholder = { Text("브랜드, 용량 등") },
                singleLine = true,
                shape = FreshKitchenShapes.Control,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            val parsedQuantity = quantityText.toDoubleOrNull()?.takeIf { it > 0.0 }
            val canSave = name.trim().isNotEmpty() && parsedQuantity != null

            Button(
                onClick = {
                    if (canSave && parsedQuantity != null) {
                        onSave(
                            name.trim(),
                            category,
                            parsedQuantity,
                            unit.trim().ifEmpty { "개" },
                            memo.trim().takeIf { it.isNotEmpty() },
                        )
                    }
                },
                enabled = canSave,
                shape = FreshKitchenShapes.Control,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshKitchenColors.PrimaryTealDeep,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(if (initialItem == null) "리스트에 추가" else "수정 저장")
            }
        }
    }
}

private fun formatPurchasedTime(isoUtc: String?): String? {
    if (isoUtc.isNullOrBlank()) return null
    return runCatching {
        val local = OffsetDateTime.parse(isoUtc)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalTime()
        local.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()
}
