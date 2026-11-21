package com.fridge.manager.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.fridge.manager.core.media.CompressedItemPhotos
import com.fridge.manager.core.media.ImageCompressor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridge.manager.core.network.CreateItemRequest
import com.fridge.manager.domain.model.ExpiryState
import com.fridge.manager.domain.model.FoodItem
import com.fridge.manager.domain.model.HouseholdInfo
import com.fridge.manager.domain.model.StorageLocationNode
import com.fridge.manager.ui.theme.FreshKitchenColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.time.LocalDate
import kotlin.math.abs

private enum class ExpiryQuickFilter(val label: String) {
    ALL("보관 상태"),
    EXPIRING("임박 품목"),
    EXPIRED("만료 품목"),
}

private data class CategoryMeta(
    val code: String,
    val label: String,
    val emoji: String,
    val tintBg: Color,
)

private val CATEGORIES = listOf(
    CategoryMeta("FRESH", "신선식품", "🥬", Color(0xFFDCFCE7)),
    CategoryMeta("MEAT_FISH", "육류/수산", "🥩", Color(0xFFFEE2E2)),
    CategoryMeta("DAIRY", "유제품", "🥛", Color(0xFFE0F2FE)),
    CategoryMeta("PROCESSED", "가공/완제품", "🍜", Color(0xFFFEF3C7)),
    CategoryMeta("BEVERAGE", "음료", "🥤", Color(0xFFEDE9FE)),
    CategoryMeta("SAUCE", "양념/소스", "🧂", Color(0xFFFFEDD5)),
    CategoryMeta("ETC", "기타", "📦", Color(0xFFF1F5F9)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    households: List<HouseholdInfo>,
    currentHouseholdId: Long?,
    locationsTree: List<StorageLocationNode>,
    flatLocations: List<StorageLocationNode>,
    items: List<FoodItem>,
    expiringOnlyInitial: Boolean,
    onSwitchHousehold: (Long) -> Unit,
    onCreateLocation: (parentId: Long?, name: String) -> Unit,
    onDeleteLocation: (locationId: Long) -> Unit,
    onCreateItem: (CreateItemRequest, CompressedItemPhotos?) -> Unit,
    onAdjustQuantity: (FoodItem, Double) -> Unit,
    onMoveItem: (itemId: Long, newLocationId: Long) -> Unit,
    onConsumeItem: (itemId: Long, amount: Double?, remainingQuantity: Double?, note: String?) -> Unit,
    onDiscardItem: (Long) -> Unit,
    onOpenProfile: () -> Unit = {},
) {
    var selectedLocationId by remember { mutableStateOf<Long?>(null) }
    var includeSubLocations by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var expiryFilter by remember(expiringOnlyInitial) {
        mutableStateOf(if (expiringOnlyInitial) ExpiryQuickFilter.EXPIRING else ExpiryQuickFilter.ALL)
    }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var sortByName by remember { mutableStateOf(false) }
    var showTreeManager by remember { mutableStateOf(false) }
    var showAddItemSheet by remember { mutableStateOf(false) }
    var showAddLocationParentId by remember { mutableStateOf<Long?>(null) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var showHouseholdMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    var moveTargetItem by remember { mutableStateOf<FoodItem?>(null) }
    var detailTargetItemId by remember { mutableStateOf<Long?>(null) }
    val detailTargetItem = remember(items, detailTargetItemId) {
        detailTargetItemId?.let { id -> items.find { it.id == id } }
    }
    val currentHouseholdName = households.find { it.id == currentHouseholdId }?.name ?: "우리집 냉장고"

    val totalCount = items.size
    val expiringCount = items.count {
        it.expiryState == ExpiryState.TODAY || it.expiryState == ExpiryState.EXPIRING_SOON
    }
    val expiredCount = items.count { it.expiryState == ExpiryState.EXPIRED }

    val allowedLocationIds = remember(selectedLocationId, includeSubLocations, flatLocations) {
        if (selectedLocationId == null) {
            null
        } else if (!includeSubLocations) {
            setOf(selectedLocationId!!)
        } else {
            collectDescendantIds(selectedLocationId!!, flatLocations)
        }
    }

    val filteredItems = remember(
        items,
        searchQuery,
        expiryFilter,
        selectedCategory,
        allowedLocationIds,
        sortByName,
    ) {
        items.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                (item.memo?.contains(searchQuery, ignoreCase = true) == true)
            val matchesExpiry = when (expiryFilter) {
                ExpiryQuickFilter.ALL -> true
                ExpiryQuickFilter.EXPIRING -> item.expiryState in listOf(
                    ExpiryState.EXPIRED,
                    ExpiryState.TODAY,
                    ExpiryState.EXPIRING_SOON,
                )
                ExpiryQuickFilter.EXPIRED -> item.expiryState == ExpiryState.EXPIRED
            }
            val matchesCat = selectedCategory == null || item.category == selectedCategory
            val matchesLoc = allowedLocationIds == null || item.locationId in allowedLocationIds
            matchesQuery && matchesExpiry && matchesCat && matchesLoc
        }.let { list ->
            if (sortByName) {
                list.sortedBy { it.name }
            } else {
                list.sortedWith(compareBy({ it.dDay ?: Int.MAX_VALUE }, { it.name }))
            }
        }
    }

    Scaffold(
        containerColor = FreshKitchenColors.CanvasBackground,
        floatingActionButton = {
            if (flatLocations.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showAddItemSheet = true },
                    containerColor = FreshKitchenColors.PrimaryTealDeep,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(999.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                        )
                    },
                    text = {
                        Text("품목 등록", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Stitch Top App Bar
            item {
                Column(modifier = Modifier.background(FreshKitchenColors.SurfaceWhite)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Kitchen,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = "우리집 냉장고",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(FreshKitchenColors.PrimaryTealDeep)
                                .clickable(onClick = onOpenProfile),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "가족·알림 설정",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    HorizontalDivider(color = FreshKitchenColors.BorderSubtle.copy(alpha = 0.7f))
                }
            }

            // Household Switcher Pill + Action Icons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Surface(
                            onClick = { if (households.size > 1) showHouseholdMenu = true },
                            shape = RoundedCornerShape(999.dp),
                            color = FreshKitchenColors.SurfaceMuted,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = currentHouseholdName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshKitchenColors.TextPrimary,
                                )
                                if (households.size > 1) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = FreshKitchenColors.TextMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        DropdownMenu(
                            expanded = showHouseholdMenu,
                            onDismissRequest = { showHouseholdMenu = false },
                        ) {
                            households.forEach { hh ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (hh.id == currentHouseholdId) "✓ ${hh.name}" else hh.name,
                                            fontWeight = if (hh.id == currentHouseholdId) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        showHouseholdMenu = false
                                        if (hh.id != currentHouseholdId) {
                                            onSwitchHousehold(hh.id)
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showTreeManager = !showTreeManager },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (showTreeManager) FreshKitchenColors.PrimarySoft else Color.Transparent,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "장소 관리",
                                tint = if (showTreeManager) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // Expiry Summary Strip (3 stat cards)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryStatCard(
                        label = "보관 중",
                        count = totalCount,
                        icon = Icons.Outlined.Kitchen,
                        bgColor = FreshKitchenColors.SurfaceMuted,
                        accentColor = FreshKitchenColors.TextPrimary,
                        iconTint = FreshKitchenColors.PrimaryTealDeep,
                        selected = expiryFilter == ExpiryQuickFilter.ALL,
                        onClick = { expiryFilter = ExpiryQuickFilter.ALL },
                        modifier = Modifier.weight(1f),
                    )
                    SummaryStatCard(
                        label = "임박 D-3",
                        count = expiringCount,
                        icon = Icons.Outlined.Timer,
                        bgColor = FreshKitchenColors.ExpiringBg,
                        accentColor = FreshKitchenColors.ExpiringText,
                        iconTint = FreshKitchenColors.ExpiringText,
                        selected = expiryFilter == ExpiryQuickFilter.EXPIRING,
                        onClick = {
                            expiryFilter = if (expiryFilter == ExpiryQuickFilter.EXPIRING) {
                                ExpiryQuickFilter.ALL
                            } else {
                                ExpiryQuickFilter.EXPIRING
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    SummaryStatCard(
                        label = "만료됨",
                        count = expiredCount,
                        icon = Icons.Rounded.PriorityHigh,
                        bgColor = FreshKitchenColors.ExpiredBg,
                        accentColor = FreshKitchenColors.ExpiredText,
                        iconTint = FreshKitchenColors.ExpiredText,
                        selected = expiryFilter == ExpiryQuickFilter.EXPIRED,
                        onClick = {
                            expiryFilter = if (expiryFilter == ExpiryQuickFilter.EXPIRED) {
                                ExpiryQuickFilter.ALL
                            } else {
                                ExpiryQuickFilter.EXPIRED
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Hierarchical Storage Location Navigator
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "보관 장소 둘러보기",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Surface(
                            onClick = { includeSubLocations = !includeSubLocations },
                            shape = RoundedCornerShape(999.dp),
                            color = if (includeSubLocations) {
                                FreshKitchenColors.PrimarySoft
                            } else {
                                FreshKitchenColors.SurfaceMuted
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (includeSubLocations) FreshKitchenColors.PrimaryTeal else FreshKitchenColors.TextMuted,
                                        ),
                                )
                                Text(
                                    text = if (includeSubLocations) "하위 구역 포함 ON" else "하위 구역 포함 OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (includeSubLocations) {
                                        FreshKitchenColors.PrimaryDark
                                    } else {
                                        FreshKitchenColors.TextMuted
                                    },
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item {
                            Surface(
                                onClick = { showTreeManager = !showTreeManager },
                                shape = RoundedCornerShape(12.dp),
                                color = if (showTreeManager) FreshKitchenColors.PrimarySoft else FreshKitchenColors.SurfaceMuted,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AccountTree,
                                        contentDescription = "트리 편집",
                                        tint = if (showTreeManager) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.TextSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                        item {
                            LocationPillChip(
                                label = "전체",
                                count = totalCount,
                                selected = selectedLocationId == null,
                                onClick = { selectedLocationId = null },
                            )
                        }
                        items(flatLocations, key = { "pill-${it.id}" }) { loc ->
                            val locIds = remember(loc.id, flatLocations) {
                                collectDescendantIds(loc.id, flatLocations)
                            }
                            val locCount = items.count { it.locationId in locIds }
                            val prefix = if (loc.depth > 0) "└ " else ""
                            LocationPillChip(
                                label = "$prefix${loc.icon ?: "📁"} ${loc.name}",
                                count = if (loc.depth == 0) locCount else null,
                                selected = selectedLocationId == loc.id,
                                onClick = {
                                    selectedLocationId = if (selectedLocationId == loc.id) null else loc.id
                                },
                            )
                        }
                    }
                }
            }

            // Optional Tree Management Panel
            if (showTreeManager) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "보관 장소 트리 편집",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                TextButton(onClick = {
                                    showAddLocationParentId = null
                                    showAddLocationDialog = true
                                }) {
                                    Text("+ 최상위 장소", color = FreshKitchenColors.PrimaryTealDeep, fontWeight = FontWeight.Bold)
                                }
                            }
                            flatLocations.forEach { loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = (loc.depth * 14).dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${loc.icon ?: "📁"} ${loc.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Row {
                                        TextButton(onClick = {
                                            showAddLocationParentId = loc.id
                                            showAddLocationDialog = true
                                        }) { Text("+하위", fontSize = 12.sp) }
                                        TextButton(onClick = { onDeleteLocation(loc.id) }) {
                                            Text("삭제", color = FreshKitchenColors.ExpiredText, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pill Search Bar & Filter Chips Row
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Stitch Pill Search Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(
                                elevation = 2.dp,
                                shape = RoundedCornerShape(999.dp),
                                ambientColor = Color.Black.copy(alpha = 0.04f),
                            )
                            .clip(RoundedCornerShape(999.dp))
                            .background(FreshKitchenColors.SurfaceWhite)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = FreshKitchenColors.TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "품목명, 메모 검색...",
                                    color = FreshKitchenColors.TextMuted,
                                    fontSize = 14.sp,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = FreshKitchenColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "검색어 지우기",
                                    tint = FreshKitchenColors.TextMuted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }

                    // Sort, Category, Status & Reset Row
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Sort pill
                        Surface(
                            onClick = { sortByName = !sortByName },
                            shape = RoundedCornerShape(999.dp),
                            color = FreshKitchenColors.PrimaryMint,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapVert,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryDark,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = if (sortByName) "이름순" else "임박순",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshKitchenColors.PrimaryDark,
                                )
                            }
                        }

                        // Category dropdown pill
                        Box {
                            val selectedCatLabel = CATEGORIES.find { it.code == selectedCategory }?.label ?: "카테고리"
                            Surface(
                                onClick = { showCategoryMenu = true },
                                shape = RoundedCornerShape(999.dp),
                                color = if (selectedCategory != null) FreshKitchenColors.PrimarySoft else FreshKitchenColors.SurfaceWhite,
                                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = selectedCatLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedCategory != null) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedCategory != null) FreshKitchenColors.PrimaryDark else FreshKitchenColors.TextPrimary,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = FreshKitchenColors.TextMuted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("전체 카테고리") },
                                    onClick = {
                                        selectedCategory = null
                                        showCategoryMenu = false
                                    },
                                )
                                CATEGORIES.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.emoji} ${cat.label}") },
                                        onClick = {
                                            selectedCategory = cat.code
                                            showCategoryMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        // Expiry status dropdown pill
                        Box {
                            Surface(
                                onClick = { showStatusMenu = true },
                                shape = RoundedCornerShape(999.dp),
                                color = if (expiryFilter != ExpiryQuickFilter.ALL) FreshKitchenColors.PrimarySoft else FreshKitchenColors.SurfaceWhite,
                                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = expiryFilter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (expiryFilter != ExpiryQuickFilter.ALL) FontWeight.Bold else FontWeight.Medium,
                                        color = if (expiryFilter != ExpiryQuickFilter.ALL) FreshKitchenColors.PrimaryDark else FreshKitchenColors.TextPrimary,
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = FreshKitchenColors.TextMuted,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showStatusMenu,
                                onDismissRequest = { showStatusMenu = false },
                            ) {
                                ExpiryQuickFilter.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(if (mode == ExpiryQuickFilter.ALL) "전체 상태" else mode.label) },
                                        onClick = {
                                            expiryFilter = mode
                                            showStatusMenu = false
                                        },
                                    )
                                }
                            }
                        }

                        // Reset filters button
                        Surface(
                            onClick = {
                                selectedLocationId = null
                                selectedCategory = null
                                expiryFilter = ExpiryQuickFilter.ALL
                                searchQuery = ""
                                sortByName = false
                            },
                            shape = CircleShape,
                            color = FreshKitchenColors.SurfaceWhite,
                            border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "필터 초기화",
                                    tint = FreshKitchenColors.TextSecondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Inventory List
            if (filteredItems.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🥬", fontSize = 36.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "조건에 맞는 식재료가 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = FreshKitchenColors.TextMuted,
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { "item-${it.id}" }) { item ->
                    FreshItemCard(
                        item = item,
                        baselineQuantity = item.initialQuantity,
                        onClick = { detailTargetItemId = item.id },
                        onAdjustQuantity = onAdjustQuantity,
                        onMoveClick = { moveTargetItem = item },
                        onConsumeItem = { id -> detailTargetItemId = id },
                        onDiscardItem = onDiscardItem,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            // Stitch Kitchen Tip Banner
            if (expiringCount > 0 || expiredCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFE0F7F4))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FreshKitchenColors.PrimaryTealDeep),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "오늘의 살림 팁",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.PrimaryDark,
                            )
                            Text(
                                text = "만료·임박 식재료 ${expiringCount + expiredCount}개를 우선 소비해 음식물 낭비를 줄여보세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddLocationDialog) {
        var locName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("보관 장소 추가", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = locName,
                    onValueChange = { locName = it },
                    label = { Text("장소 이름 (예: 야채칸, 1단 선반)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (locName.isNotBlank()) {
                            onCreateLocation(showAddLocationParentId, locName.trim())
                            showAddLocationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FreshKitchenColors.PrimaryTealDeep),
                ) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) { Text("취소") }
            },
        )
    }

    if (moveTargetItem != null && flatLocations.isNotEmpty()) {
        val target = moveTargetItem!!
        AlertDialog(
            onDismissRequest = { moveTargetItem = null },
            title = { Text("보관 장소 이동: ${target.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    flatLocations.forEach { loc ->
                        val isCurrent = loc.id == target.locationId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) FreshKitchenColors.PrimarySoft else FreshKitchenColors.SurfaceMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCurrent) {
                                    onMoveItem(target.id, loc.id)
                                    moveTargetItem = null
                                },
                        ) {
                            Text(
                                text = "${"  ".repeat(loc.depth)}${loc.icon ?: "📁"} ${loc.name}" +
                                    if (isCurrent) " (현재)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) FreshKitchenColors.PrimaryDark else FreshKitchenColors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { moveTargetItem = null }) { Text("닫기") }
            },
        )
    }

    if (showAddItemSheet && flatLocations.isNotEmpty()) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAddItemSheet = false },
            sheetState = sheetState,
            containerColor = FreshKitchenColors.SurfaceWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFCBD5E1)),
                )
            },
        ) {
            AddItemSheetContent(
                flatLocations = flatLocations,
                initialLocationId = selectedLocationId ?: flatLocations.first().id,
                onDismiss = { showAddItemSheet = false },
                onSubmit = { req, photos ->
                    onCreateItem(req, photos)
                    showAddItemSheet = false
                },
            )
        }
    }

    if (detailTargetItem != null) {
        val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { detailTargetItemId = null },
            sheetState = detailSheetState,
            containerColor = FreshKitchenColors.SurfaceWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFFCBD5E1)),
                )
            },
        ) {
            FoodItemDetailSheetContent(
                item = detailTargetItem,
                onDismiss = { detailTargetItemId = null },
                onAdjustQuantity = onAdjustQuantity,
                onMoveClick = { moveTargetItem = detailTargetItem },
                onRecordRemaining = { id, remainingQuantity, note ->
                    if (remainingQuantity <= 0.0) {
                        detailTargetItemId = null
                    }
                    onConsumeItem(id, null, remainingQuantity, note)
                },
                onConsumeItem = { id ->
                    detailTargetItemId = null
                    onConsumeItem(id, null, null, null)
                },
                onDiscardItem = { id ->
                    detailTargetItemId = null
                    onDiscardItem(id)
                },
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    count: Int,
    icon: ImageVector,
    bgColor: Color,
    accentColor: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = if (selected) {
            BorderStroke(1.5.dp, FreshKitchenColors.PrimaryTealDeep)
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = FreshKitchenColors.TextSecondary,
                    maxLines = 1,
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)) {
                        append(count.toString())
                    }
                    withStyle(SpanStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accentColor.copy(alpha = 0.8f))) {
                        append(" 개")
                    }
                },
            )
        }
    }
}

@Composable
private fun LocationPillChip(
    label: String,
    count: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.SurfaceWhite,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.BorderSubtle,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else FreshKitchenColors.TextPrimary,
                maxLines = 1,
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) Color.White.copy(alpha = 0.22f) else FreshKitchenColors.SurfaceMuted,
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else FreshKitchenColors.TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

private data class ExpiryBadgeStyle(
    val text: String,
    val bgColor: Color,
    val textColor: Color,
    val typeLabel: String,
)

private fun resolveExpiryBadgeStyle(item: FoodItem): ExpiryBadgeStyle {
    val expiryTypeLabel = when (item.expiryType) {
        "BEST_BEFORE" -> "유통기한"
        "USE_BY" -> "소비기한"
        else -> ""
    }
    return when (item.expiryState) {
        ExpiryState.EXPIRED -> ExpiryBadgeStyle(
            text = "D+${abs(item.dDay ?: 0)} 만료",
            bgColor = FreshKitchenColors.ExpiredBg,
            textColor = FreshKitchenColors.ExpiredText,
            typeLabel = expiryTypeLabel,
        )
        ExpiryState.TODAY -> ExpiryBadgeStyle(
            text = "D-Day",
            bgColor = FreshKitchenColors.TodayBg,
            textColor = FreshKitchenColors.TodayText,
            typeLabel = expiryTypeLabel,
        )
        ExpiryState.EXPIRING_SOON -> ExpiryBadgeStyle(
            text = "D-${item.dDay} $expiryTypeLabel".trim(),
            bgColor = FreshKitchenColors.ExpiringBg,
            textColor = FreshKitchenColors.ExpiringText,
            typeLabel = expiryTypeLabel,
        )
        ExpiryState.SAFE -> ExpiryBadgeStyle(
            text = "D-${item.dDay} $expiryTypeLabel".trim(),
            bgColor = FreshKitchenColors.SafeBg,
            textColor = FreshKitchenColors.SafeText,
            typeLabel = expiryTypeLabel,
        )
        ExpiryState.NONE -> ExpiryBadgeStyle(
            text = "기한 없음",
            bgColor = FreshKitchenColors.NoneBg,
            textColor = FreshKitchenColors.NoneText,
            typeLabel = expiryTypeLabel,
        )
    }
}

private fun formatQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        quantity.toString()
    }
}

@Composable
private fun FreshItemCard(
    item: FoodItem,
    baselineQuantity: Double = item.quantity,
    onClick: () -> Unit,
    onAdjustQuantity: (FoodItem, Double) -> Unit,
    onMoveClick: () -> Unit,
    onConsumeItem: (Long) -> Unit,
    onDiscardItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val catMeta = CATEGORIES.find { it.code == item.category } ?: CATEGORIES.last()
    val badgeStyle = resolveExpiryBadgeStyle(item)
    val dDayText = badgeStyle.text
    val badgeBg = badgeStyle.bgColor
    val badgeText = badgeStyle.textColor

    val topAccentColor = when (item.expiryState) {
        ExpiryState.EXPIRED -> FreshKitchenColors.ExpiredText
        ExpiryState.TODAY -> FreshKitchenColors.TodayText
        ExpiryState.EXPIRING_SOON -> Color(0xFFFACC15)
        else -> Color.Transparent
    }

    val qtyDisplay = formatQuantity(item.quantity)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f),
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = FreshKitchenColors.SurfaceWhite,
        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle.copy(alpha = 0.6f)),
    ) {
        Column {
            if (topAccentColor != Color.Transparent) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(topAccentColor),
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(catMeta.tintBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!item.thumbImageUrl.isNullOrBlank()) {
                            RemoteWebpImage(
                                imageUrl = item.thumbImageUrl,
                                contentDescription = item.name,
                                modifier = Modifier.fillMaxSize(),
                                fallback = { Text(catMeta.emoji, fontSize = 23.sp) },
                            )
                        } else {
                            Text(catMeta.emoji, fontSize = 23.sp)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshKitchenColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (item.expiryState == ExpiryState.EXPIRED) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = FreshKitchenColors.ExpiredBg,
                                    ) {
                                        Text(
                                            text = "확인필요",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FreshKitchenColors.ExpiredText,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeBg,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                ) {
                                    if (item.expiryState == ExpiryState.EXPIRED) {
                                        Icon(
                                            imageVector = Icons.Rounded.WarningAmber,
                                            contentDescription = null,
                                            tint = badgeText,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    } else if (item.expiryState == ExpiryState.NONE) {
                                        Icon(
                                            imageVector = Icons.Rounded.AllInclusive,
                                            contentDescription = null,
                                            tint = badgeText,
                                            modifier = Modifier.size(12.dp),
                                        )
                                    }
                                    Text(
                                        text = dDayText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeText,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = null,
                                tint = FreshKitchenColors.TextMuted,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = item.locationPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (item.expiryState == ExpiryState.EXPIRED) {
                            Text(
                                text = "상태를 확인하고 우선 소비하거나 정리하세요.",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshKitchenColors.ExpiredText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        } else if (!item.memo.isNullOrBlank()) {
                            Text(
                                text = item.memo,
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Remaining Quantity ProgressBar
                val safeBaseline = baselineQuantity.coerceAtLeast(item.quantity).coerceAtLeast(0.01)
                val remainingRatio = (item.quantity / safeBaseline).toFloat().coerceIn(0f, 1f)
                val remainingPercent = (remainingRatio * 100f).roundToInt()
                val remainingBarColor = when {
                    remainingPercent <= 25 -> FreshKitchenColors.ExpiredText
                    remainingPercent <= 50 -> FreshKitchenColors.ExpiringText
                    else -> FreshKitchenColors.PrimaryTealDeep
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "남은 양",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = FreshKitchenColors.TextMuted,
                        )
                        Text(
                            text = "$qtyDisplay / ${formatQuantity(safeBaseline)} ${item.unit} · $remainingPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = remainingBarColor,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(FreshKitchenColors.SurfaceMuted),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(remainingRatio)
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(remainingBarColor),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Single-line Quick Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Compact Pill Stepper
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(FreshKitchenColors.SurfaceMuted)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAdjustQuantity(item, -1.0) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "−",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Text(
                            text = "$qtyDisplay ${item.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshKitchenColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAdjustQuantity(item, 1.0) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                    }

                    // Compact Action Pills (Move / Discard / Consume)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ActionPillButton(
                            label = "이동",
                            icon = Icons.AutoMirrored.Rounded.DriveFileMove,
                            bgColor = FreshKitchenColors.SurfaceMuted,
                            contentColor = FreshKitchenColors.TextPrimary,
                            onClick = onMoveClick,
                        )
                        val isExpired = item.expiryState == ExpiryState.EXPIRED
                        ActionPillButton(
                            label = "폐기",
                            icon = Icons.Outlined.DeleteOutline,
                            bgColor = if (isExpired) FreshKitchenColors.ExpiredText else FreshKitchenColors.ExpiredBg,
                            contentColor = if (isExpired) Color.White else FreshKitchenColors.ExpiredText,
                            onClick = { onDiscardItem(item.id) },
                        )
                        ActionPillButton(
                            label = "소비",
                            icon = Icons.Rounded.Check,
                            bgColor = FreshKitchenColors.PrimaryTealDeep,
                            contentColor = Color.White,
                            onClick = { onConsumeItem(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPillButton(
    label: String,
    icon: ImageVector,
    bgColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private enum class PhotoScanMode(val label: String, val hint: String) {
    INGREDIENT("식재료 사진", "실물 식재료를 찍어 2단계 WebP(1280px/400px)로 저장합니다."),
    RECEIPT("영수증", "영수증을 촬영해 구매 내역 참고용 이미지로 함께 보관합니다."),
    BARCODE("바코드", "상품 바코드 라벨을 촬영해 품목 이미지로 첨부합니다."),
}

@Composable
private fun AddItemSheetContent(
    flatLocations: List<StorageLocationNode>,
    initialLocationId: Long,
    onDismiss: () -> Unit,
    onSubmit: (CreateItemRequest, CompressedItemPhotos?) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var locationId by remember { mutableStateOf(initialLocationId) }
    var category by remember { mutableStateOf("FRESH") }
    var qty by remember { mutableStateOf(1.0) }
    var unit by remember { mutableStateOf("개") }
    var expiryType by remember { mutableStateOf("USE_BY") }
    var expiryDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var memo by remember { mutableStateOf("") }
    var scanMode by remember { mutableStateOf(PhotoScanMode.INGREDIENT) }
    var compressedPhotos by remember { mutableStateOf<CompressedItemPhotos?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val handlePickedUri: (Uri) -> Unit = { uri ->
        runCatching {
            ImageCompressor.compressFromUri(context, uri)
        }.onSuccess { photos ->
            compressedPhotos = photos
            photoError = null
        }.onFailure { err ->
            photoError = err.message ?: "이미지를 처리할 수 없습니다."
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            handlePickedUri(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            handlePickedUri(uri)
        }
    }

    val selectedLoc = flatLocations.find { it.id == locationId } ?: flatLocations.first()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = FreshKitchenColors.PrimaryTealDeep,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "새 품목 등록",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FreshKitchenColors.TextPrimary,
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FreshKitchenColors.SurfaceMuted)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "닫기",
                    tint = FreshKitchenColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Photo Registration Section (Optional)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.SurfaceMuted,
            border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "사진으로 등록 (선택)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FreshKitchenColors.TextPrimary,
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = FreshKitchenColors.PrimarySoft,
                    ) {
                        Text(
                            text = "WebP 자동 압축",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Scan Mode Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    PhotoScanMode.entries.forEach { mode ->
                        val selected = scanMode == mode
                        Surface(
                            onClick = { scanMode = mode },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (selected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.BorderSubtle,
                            ),
                        ) {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else FreshKitchenColors.TextPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Text(
                    text = scanMode.hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = FreshKitchenColors.TextMuted,
                )

                // Camera / Gallery Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = {
                            runCatching {
                                val photoDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
                                val photoFile = File.createTempFile("item_", ".jpg", photoDir)
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile,
                                )
                            }.onSuccess { uri ->
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            }.onFailure { err ->
                                photoError = err.message ?: "카메라를 실행할 수 없습니다."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "카메라 촬영",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                    }

                    Surface(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 11.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "앨범에서 선택",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                    }
                }

                // Preview Card
                val previewPhotos = compressedPhotos
                if (previewPhotos != null) {
                    val thumbBitmap = remember(previewPhotos) {
                        BitmapFactory.decodeByteArray(
                            previewPhotos.thumbBytes,
                            0,
                            previewPhotos.thumbBytes.size,
                        )?.asImageBitmap()
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.25f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (thumbBitmap != null) {
                                Image(
                                    bitmap = thumbBitmap,
                                    contentDescription = "선택된 사진 미리보기",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = "${scanMode.label} 첨부 준비 완료",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshKitchenColors.PrimaryTealDeep,
                                )
                                Text(
                                    text = "상세 ${(previewPhotos.detailBytes.size / 1024).coerceAtLeast(1)}KB · 썸네일 ${(previewPhotos.thumbBytes.size / 1024).coerceAtLeast(1)}KB (WebP)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FreshKitchenColors.TextMuted,
                                )
                            }
                            IconButton(
                                onClick = { compressedPhotos = null },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "사진 제거",
                                    tint = FreshKitchenColors.TextMuted,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                } else if (photoError != null) {
                    Text(
                        text = photoError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshKitchenColors.ExpiredText,
                    )
                }
            }
        }

        // Item Name
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "품목명 *",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = FreshKitchenColors.TextPrimary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(FreshKitchenColors.SurfaceMuted)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (name.isEmpty()) {
                        Text(
                            text = "예: 서울우유 1L, 대파 1단",
                            color = FreshKitchenColors.TextMuted,
                            fontSize = 15.sp,
                        )
                    }
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = FreshKitchenColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (name.isNotEmpty()) {
                    IconButton(onClick = { name = "" }, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "지우기",
                            tint = FreshKitchenColors.TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Storage Location (Mint-tinted Card with Grid Chips)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "보관 장소 *",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FreshKitchenColors.TextPrimary,
                )
                Text(
                    text = "장소 선택 가능",
                    style = MaterialTheme.typography.labelSmall,
                    color = FreshKitchenColors.TextMuted,
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = FreshKitchenColors.MintGlow,
                border = BorderStroke(1.dp, FreshKitchenColors.MintBorder),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Breadcrumb pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = selectedLoc.icon ?: "📁",
                            fontSize = 15.sp,
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = FreshKitchenColors.PrimarySoft,
                        ) {
                            Text(
                                text = selectedLoc.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.PrimaryDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }

                    Text(
                        text = "빠른 위치 변경",
                        style = MaterialTheme.typography.labelSmall,
                        color = FreshKitchenColors.TextMuted,
                    )

                    // 2-column grid of locations
                    val rows = flatLocations.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                pair.forEach { loc ->
                                    val isSel = loc.id == locationId
                                    Surface(
                                        onClick = { locationId = loc.id },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.SurfaceWhite,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSel) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.BorderSubtle,
                                        ),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            if (isSel) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = loc.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else FreshKitchenColors.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category Chips
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "카테고리",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = FreshKitchenColors.TextPrimary,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CATEGORIES.forEach { cat ->
                    LocationPillChip(
                        label = "${cat.emoji} ${cat.label}",
                        selected = category == cat.code,
                        onClick = { category = cat.code },
                    )
                }
            }
        }

        // Quantity & Unit
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "수량 및 단위",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = FreshKitchenColors.TextPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Stitch stepper with teal + button
                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FreshKitchenColors.SurfaceMuted)
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(FreshKitchenColors.SurfaceWhite)
                            .clickable { if (qty > 1.0) qty -= 1.0 },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("−", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)) {
                                append(if (qty % 1.0 == 0.0) qty.toInt().toString() else qty.toString())
                            }
                            withStyle(SpanStyle(fontSize = 12.sp, color = FreshKitchenColors.TextMuted)) {
                                append(" 수량")
                            }
                        },
                    )
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(FreshKitchenColors.PrimaryTealDeep)
                            .clickable { qty += 1.0 },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                }

                // Unit soft input
                Row(
                    modifier = Modifier
                        .weight(0.9f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FreshKitchenColors.SurfaceMuted)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "단위:",
                        style = MaterialTheme.typography.labelMedium,
                        color = FreshKitchenColors.TextMuted,
                    )
                    Spacer(Modifier.width(6.dp))
                    BasicTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = FreshKitchenColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "자주 쓰는 단위:",
                    style = MaterialTheme.typography.labelSmall,
                    color = FreshKitchenColors.TextMuted,
                )
                listOf("개", "단", "봉", "팩", "병", "g", "kg", "ml").forEach { preset ->
                    val isSel = unit == preset
                    Surface(
                        onClick = { unit = preset },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) FreshKitchenColors.PrimarySoft else FreshKitchenColors.SurfaceMuted,
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) FreshKitchenColors.PrimaryDark else FreshKitchenColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }

        // Expiry Type Segmented Pill & Date
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.CanvasBackground,
            border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = "기한 관리",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.TextPrimary,
                        )
                    }
                    Text(
                        text = if (expiryType == "NONE") "기한 없음" else "D-7 기본 추천",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FreshKitchenColors.PrimaryTealDeep,
                    )
                }

                // 3-tab Segmented Track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(FreshKitchenColors.SurfaceMuted)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("USE_BY" to "소비기한", "BEST_BEFORE" to "유통기한", "NONE" to "기한 없음").forEach { (code, label) ->
                        val isSel = expiryType == code
                        Surface(
                            onClick = { expiryType = code },
                            shape = RoundedCornerShape(9.dp),
                            color = if (isSel) FreshKitchenColors.SurfaceWhite else Color.Transparent,
                            shadowElevation = if (isSel) 1.dp else 0.dp,
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) FreshKitchenColors.TextPrimary else FreshKitchenColors.TextMuted,
                                )
                            }
                        }
                    }
                }

                if (expiryType != "NONE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(FreshKitchenColors.SurfaceWhite)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = FreshKitchenColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3L to "+3일", 7L to "+1주", 30L to "+1개월").forEach { (days, label) ->
                            Surface(
                                onClick = { expiryDate = LocalDate.now().plusDays(days).toString() },
                                shape = RoundedCornerShape(999.dp),
                                color = FreshKitchenColors.SurfaceWhite,
                                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FreshKitchenColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Memo (optional)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(FreshKitchenColors.SurfaceMuted)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (memo.isEmpty()) {
                    Text(
                        text = "메모 (선택, 예: 소분 밀폐 포장)",
                        color = FreshKitchenColors.TextMuted,
                        fontSize = 14.sp,
                    )
                }
                BasicTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = FreshKitchenColors.TextPrimary,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Button(
            onClick = {
                if (name.isNotBlank()) {
                    onSubmit(
                        CreateItemRequest(
                            locationId = locationId,
                            name = name.trim(),
                            category = category,
                            quantity = qty,
                            unit = unit.ifBlank { "개" },
                            expiryDate = if (expiryType == "NONE") null else expiryDate.trim().ifBlank { null },
                            expiryType = expiryType,
                            memo = memo.trim().ifBlank { null },
                        ),
                        compressedPhotos,
                    )
                }
            },
            enabled = name.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FreshKitchenColors.PrimaryTealDeep,
                contentColor = Color.White,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("등록하기", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = FreshKitchenColors.TextMuted,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "등록 시 가족 구성원에게 알림이 전송됩니다.",
                style = MaterialTheme.typography.labelSmall,
                color = FreshKitchenColors.TextMuted,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FoodItemDetailSheetContent(
    item: FoodItem,
    onDismiss: () -> Unit,
    onAdjustQuantity: (FoodItem, Double) -> Unit,
    onMoveClick: () -> Unit,
    onRecordRemaining: (itemId: Long, remainingQuantity: Double, note: String?) -> Unit,
    onConsumeItem: (Long) -> Unit,
    onDiscardItem: (Long) -> Unit,
) {
    val catMeta = CATEGORIES.find { it.code == item.category } ?: CATEGORIES.last()
    val badgeStyle = resolveExpiryBadgeStyle(item)
    val qtyDisplay = formatQuantity(item.quantity)

    val statusLabel = when (item.status) {
        "CONSUMED" -> "소비 완료"
        "DISCARDED" -> "폐기됨"
        else -> "보관 중"
    }

    val (urgencyHint, progressRatio, progressColor) = when (item.expiryState) {
        ExpiryState.EXPIRED -> Triple(
            "${abs(item.dDay ?: 0)}일 경과 (확인 필요)",
            1f,
            FreshKitchenColors.ExpiredText,
        )
        ExpiryState.TODAY -> Triple(
            "오늘 마감",
            0.95f,
            FreshKitchenColors.TodayText,
        )
        ExpiryState.EXPIRING_SOON -> Triple(
            "${item.dDay ?: 0}일 후 마감 예정",
            0.78f,
            FreshKitchenColors.ExpiringText,
        )
        ExpiryState.SAFE -> Triple(
            "${item.dDay ?: 0}일 남음 (여유)",
            0.32f,
            FreshKitchenColors.SafeText,
        )
        ExpiryState.NONE -> Triple(
            "기한 미설정",
            0f,
            FreshKitchenColors.NoneText,
        )
    }

    val expiryTypeTitle = when (item.expiryType) {
        "BEST_BEFORE" -> "유통기한 기준"
        "USE_BY" -> "소비기한 기준"
        else -> "기한 없음"
    }

    val locationSegments = remember(item.locationPath) {
        item.locationPath.split(">").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header: Category Icon + Badges + Name + Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(catMeta.tintBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(catMeta.emoji, fontSize = 26.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FreshKitchenColors.PrimarySoft,
                    ) {
                        Text(
                            text = catMeta.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeStyle.bgColor,
                    ) {
                        Text(
                            text = badgeStyle.text,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeStyle.textColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = FreshKitchenColors.TextPrimary,
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FreshKitchenColors.SurfaceMuted),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "닫기",
                    tint = FreshKitchenColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val highResUrl = item.detailImageUrl ?: item.thumbImageUrl
        if (!highResUrl.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(16.dp),
                color = FreshKitchenColors.SurfaceMuted,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
            ) {
                RemoteWebpImage(
                    imageUrl = highResUrl,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(catMeta.emoji, fontSize = 40.sp)
                        }
                    },
                )
            }
        }

        // 1. Current Quantity Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.SurfaceMuted,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "현재 보유 수량",
                        style = MaterialTheme.typography.labelMedium,
                        color = FreshKitchenColors.TextMuted,
                    )
                    Text(
                        text = "$qtyDisplay ${item.unit}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = FreshKitchenColors.TextPrimary,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = FreshKitchenColors.SurfaceWhite,
                    border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FreshKitchenColors.SurfaceMuted)
                                .clickable { onAdjustQuantity(item, -1.0) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "−",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Text(
                            text = qtyDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshKitchenColors.TextPrimary,
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(FreshKitchenColors.PrimarySoft)
                                .clickable { onAdjustQuantity(item, 1.0) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }
                }
            }
        }

        // 1.5 Inline Partial Consumption & Remaining Quantity Card
        if (item.status == "ACTIVE" && item.quantity > 0.0) {
            PartialConsumeInlineCard(
                item = item,
                onRecordRemaining = { remaining, note ->
                    onRecordRemaining(item.id, remaining, note)
                },
            )
        }

        // 2. Storage Location Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.SurfaceMuted,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Place,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "보관 장소",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.TextPrimary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = FreshKitchenColors.SurfaceWhite,
                        border = BorderStroke(1.dp, FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onMoveClick),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.DriveFileMove,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "장소 이동",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = FreshKitchenColors.SurfaceWhite,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            if (locationSegments.isEmpty()) {
                                append(item.locationPath)
                            } else {
                                locationSegments.forEachIndexed { idx, segment ->
                                    val isLast = idx == locationSegments.lastIndex
                                    withStyle(
                                        SpanStyle(
                                            color = if (isLast) {
                                                FreshKitchenColors.PrimaryTealDeep
                                            } else {
                                                FreshKitchenColors.TextSecondary
                                            },
                                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                    ) {
                                        append(segment)
                                    }
                                    if (!isLast) {
                                        withStyle(SpanStyle(color = FreshKitchenColors.TextMuted)) {
                                            append("  ›  ")
                                        }
                                    }
                                }
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // 3. Expiry Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.SurfaceMuted,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = badgeStyle.textColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "기한 정보",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.TextPrimary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = FreshKitchenColors.PrimarySoft,
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = FreshKitchenColors.SurfaceWhite,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = expiryTypeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = FreshKitchenColors.TextMuted,
                            )
                            Text(
                                text = urgencyHint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = badgeStyle.textColor,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = item.expiryDate ?: "날짜 미지정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshKitchenColors.TextPrimary,
                            )
                            Text(
                                text = badgeStyle.text,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                        }

                        if (item.expiryState != ExpiryState.NONE) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(FreshKitchenColors.SurfaceMuted),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressRatio)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(progressColor),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Memo Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = FreshKitchenColors.SurfaceMuted,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = FreshKitchenColors.TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "메모",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FreshKitchenColors.TextPrimary,
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = FreshKitchenColors.SurfaceWhite,
                ) {
                    Text(
                        text = item.memo?.takeIf { it.isNotBlank() } ?: "등록된 메모가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.memo.isNullOrBlank()) {
                            FreshKitchenColors.TextMuted
                        } else {
                            FreshKitchenColors.TextPrimary
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // 5. Bottom Action Bar (Discard / Consume Complete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onDiscardItem(item.id) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshKitchenColors.ExpiredBg,
                    contentColor = FreshKitchenColors.ExpiredText,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("폐기", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = { onConsumeItem(item.id) },
                modifier = Modifier
                    .weight(1.3f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FreshKitchenColors.PrimaryTealDeep,
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("소비 완료", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

private fun formatQuantityValue(value: Double): String {
    val rounded = (value * 100.0).roundToInt() / 100.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toLong().toString()
    } else {
        rounded.toString().trimEnd('0').trimEnd('.')
    }
}

@Composable
private fun PartialConsumeInlineCard(
    item: FoodItem,
    onRecordRemaining: (remainingQuantity: Double, note: String?) -> Unit,
) {
    val totalQty = item.quantity.coerceAtLeast(0.0)
    val initialConsumed = ((totalQty * 0.5) * 100.0).roundToInt() / 100.0
    val initialRemaining = ((totalQty - initialConsumed) * 100.0).roundToInt() / 100.0

    var selectedPresetIndex by remember(item.id, item.quantity) { mutableStateOf<Int?>(1) }
    var consumedText by remember(item.id, item.quantity) { mutableStateOf(formatQuantityValue(initialConsumed)) }
    var remainingText by remember(item.id, item.quantity) { mutableStateOf(formatQuantityValue(initialRemaining)) }
    var noteText by remember(item.id) { mutableStateOf("") }

    val parsedRemaining = remainingText.toDoubleOrNull()?.coerceIn(0.0, totalQty)
    val parsedConsumed = consumedText.toDoubleOrNull()?.coerceIn(0.0, totalQty)
    val effectiveRemaining = parsedRemaining ?: (totalQty - (parsedConsumed ?: 0.0)).coerceAtLeast(0.0)
    val effectiveConsumed = (totalQty - effectiveRemaining).coerceAtLeast(0.0)
    val remainingRatio = if (totalQty > 0.0) (effectiveRemaining / totalQty).toFloat().coerceIn(0f, 1f) else 0f
    val remainingPercent = (remainingRatio * 100f).roundToInt()
    val isValid = parsedRemaining != null && effectiveConsumed > 0.0

    val presets = listOf(
        ("1/4 먹음" to "75% 남음") to 0.25,
        ("절반 먹음" to "50% 남음") to 0.50,
        ("3/4 먹음" to "25% 남음") to 0.75,
        ("전부 먹음" to "소비 완료") to 1.00,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FreshKitchenColors.SurfaceWhite,
        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = FreshKitchenColors.PrimaryTealDeep,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "먹고 남긴 양 기록 (부분 소비)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FreshKitchenColors.TextPrimary,
                )
            }

            // Quick Ratio Chips (2x2)
            presets.chunked(2).forEachIndexed { rowIdx, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowItems.forEachIndexed { colIdx, (labels, consumeRatio) ->
                        val idx = rowIdx * 2 + colIdx
                        val (title, sub) = labels
                        val isSelected = selectedPresetIndex == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPresetIndex = idx
                                    val c = ((totalQty * consumeRatio) * 100.0).roundToInt() / 100.0
                                    val r = ((totalQty - c) * 100.0).roundToInt() / 100.0
                                    consumedText = formatQuantityValue(c)
                                    remainingText = formatQuantityValue(r.coerceAtLeast(0.0))
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.SurfaceWhite,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.BorderSubtle,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else FreshKitchenColors.TextPrimary,
                                )
                                Text(
                                    text = sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else FreshKitchenColors.TextMuted,
                                )
                            }
                        }
                    }
                }
            }

            // 2-column synced inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "먹은 양 (${item.unit})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FreshKitchenColors.TextMuted,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshKitchenColors.SurfaceMuted,
                    ) {
                        BasicTextField(
                            value = consumedText,
                            onValueChange = { raw ->
                                selectedPresetIndex = null
                                consumedText = raw
                                val num = raw.toDoubleOrNull()
                                if (num != null && num >= 0.0) {
                                    val rem = (totalQty - num).coerceIn(0.0, totalQty)
                                    remainingText = formatQuantityValue(rem)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshKitchenColors.TextPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "남은 양 (${item.unit})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = FreshKitchenColors.PrimaryTealDeep,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = FreshKitchenColors.SurfaceMuted,
                    ) {
                        BasicTextField(
                            value = remainingText,
                            onValueChange = { raw ->
                                selectedPresetIndex = null
                                remainingText = raw
                                val num = raw.toDoubleOrNull()
                                if (num != null && num >= 0.0) {
                                    val cons = (totalQty - num).coerceIn(0.0, totalQty)
                                    consumedText = formatQuantityValue(cons)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            // Mint preview banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = FreshKitchenColors.PrimarySoft,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = if (effectiveRemaining <= 0.0) {
                            "전량 소비 완료 처리됩니다"
                        } else {
                            "기록 후 남은 양: ${formatQuantityValue(effectiveRemaining)} ${item.unit} ($remainingPercent% 남음)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = FreshKitchenColors.PrimaryTealDeep,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.7f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(remainingRatio)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(FreshKitchenColors.PrimaryTealDeep),
                        )
                    }
                }
            }

            // Optional note + inline Record button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = FreshKitchenColors.SurfaceMuted,
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
                        if (noteText.isEmpty()) {
                            Text(
                                text = "예: 반만 먹고 냉장 보관",
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextMuted,
                            )
                        }
                        BasicTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = FreshKitchenColors.TextPrimary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Button(
                    onClick = {
                        val note = noteText.trim().takeIf { it.isNotEmpty() }
                        onRecordRemaining(effectiveRemaining, note)
                        noteText = ""
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshKitchenColors.PrimaryTealDeep,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text("남은 양 기록", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RemoteWebpImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {},
) {
    var loadedBitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageUrl) {
        loadedBitmap = withContext(Dispatchers.IO) {
            runCatching {
                URL(imageUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val bmp = loadedBitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        fallback()
    }
}

private fun collectDescendantIds(
    rootId: Long,
    flatLocations: List<StorageLocationNode>,
): Set<Long> {
    val result = mutableSetOf(rootId)
    val childrenByParent = flatLocations.groupBy { it.parentId }
    val queue = ArrayDeque<Long>()
    queue.add(rootId)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        childrenByParent[current].orEmpty().forEach { child ->
            if (result.add(child.id)) {
                queue.add(child.id)
            }
        }
    }
    return result
}
