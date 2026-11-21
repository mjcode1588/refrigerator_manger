package com.fridge.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridge.manager.core.notification.NotificationHelper
import com.fridge.manager.domain.model.ExpiryState
import com.fridge.manager.ui.main.MainViewModel
import com.fridge.manager.ui.screens.AuthOnboardingScreen
import com.fridge.manager.ui.screens.FamilyAndSettingsScreen
import com.fridge.manager.ui.screens.InventoryScreen
import com.fridge.manager.ui.theme.FreshKitchenColors
import com.fridge.manager.ui.theme.FridgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openExpiring = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_EXPIRING, false) ?: false

        setContent {
            FridgeTheme {
                val vm: MainViewModel = hiltViewModel()
                val token by vm.isLoggedIn.collectAsState()
                val householdId by vm.currentHouseholdId.collectAsState()
                val households by vm.households.collectAsState()
                val nickname by vm.nickname.collectAsState()
                val locationsTree by vm.locationTree.collectAsState()
                val flatLocations by vm.flatLocations.collectAsState()
                val activeItems by vm.activeItems.collectAsState()
                val activities by vm.activities.collectAsState()
                val inviteCode by vm.inviteCode.collectAsState()
                val notificationSettings by vm.notificationSettings.collectAsState()
                val message by vm.message.collectAsState()
                val isLoginLoading by vm.isLoginLoading.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTab by remember { mutableIntStateOf(0) }
                val hasUrgentAlerts = activeItems.any {
                    it.expiryState == ExpiryState.EXPIRED ||
                        it.expiryState == ExpiryState.TODAY ||
                        it.expiryState == ExpiryState.EXPIRING_SOON
                }

                LaunchedEffect(message) {
                    message?.let {
                        snackbarHostState.showSnackbar(it)
                        vm.clearMessage()
                    }
                }

                LaunchedEffect(householdId) {
                    if (householdId != null) {
                        vm.refreshAll()
                    }
                }

                if (token.isNullOrBlank() || householdId == null) {
                    Scaffold(
                        containerColor = FreshKitchenColors.CanvasBackground,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            AuthOnboardingScreen(
                                isLoggedIn = !token.isNullOrBlank(),
                                isLoggingIn = isLoginLoading,
                                onDevLogin = vm::devLogin,
                                onCreateHousehold = vm::createHousehold,
                                onJoinHousehold = vm::joinHousehold,
                            )
                        }
                    }
                } else {
                    Scaffold(
                        containerColor = FreshKitchenColors.CanvasBackground,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            Column {
                                HorizontalDivider(thickness = 1.dp, color = FreshKitchenColors.BorderSubtle)
                                NavigationBar(
                                    containerColor = FreshKitchenColors.SurfaceWhite,
                                    tonalElevation = 0.dp,
                                ) {
                                    val itemColors = NavigationBarItemDefaults.colors(
                                        indicatorColor = FreshKitchenColors.PrimaryMint,
                                        selectedIconColor = FreshKitchenColors.PrimaryDark,
                                        selectedTextColor = FreshKitchenColors.PrimaryDark,
                                        unselectedIconColor = FreshKitchenColors.TextMuted,
                                        unselectedTextColor = FreshKitchenColors.TextMuted,
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Kitchen,
                                                contentDescription = "재고·장소",
                                            )
                                        },
                                        label = {
                                            Text(
                                                "재고·장소",
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        },
                                        colors = itemColors,
                                    )
                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = {
                                            Box {
                                                Icon(
                                                    imageVector = Icons.Rounded.Group,
                                                    contentDescription = "가족·알림",
                                                )
                                                if (hasUrgentAlerts) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 3.dp, y = (-2).dp)
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(FreshKitchenColors.ExpiredText),
                                                    )
                                                }
                                            }
                                        },
                                        label = {
                                            Text(
                                                "가족·알림",
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                            )
                                        },
                                        colors = itemColors,
                                    )
                                }
                            }
                        },
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            if (selectedTab == 0) {
                                InventoryScreen(
                                    households = households,
                                    currentHouseholdId = householdId,
                                    locationsTree = locationsTree,
                                    flatLocations = flatLocations,
                                    items = activeItems,
                                    expiringOnlyInitial = openExpiring,
                                    onSwitchHousehold = vm::switchHousehold,
                                    onCreateLocation = vm::createLocation,
                                    onDeleteLocation = vm::deleteLocation,
                                    onCreateItem = { req, photos -> vm.createItem(req, photos) },
                                    onAdjustQuantity = vm::adjustQuantity,
                                    onMoveItem = vm::moveItem,
                                    onConsumeItem = { id, amount, remainingQuantity, note ->
                                        vm.consumeItem(id, amount, remainingQuantity, note)
                                    },
                                    onDiscardItem = vm::discardItem,
                                    onOpenProfile = { selectedTab = 1 },
                                )
                            } else {
                                FamilyAndSettingsScreen(
                                    nickname = nickname,
                                    householdName = households.find { it.id == householdId }?.name ?: "행복한 우리집 냉장고",
                                    inviteCode = inviteCode,
                                    activities = activities,
                                    notificationSettings = notificationSettings,
                                    onGenerateInvite = vm::generateInviteCode,
                                    onUpdateNotificationSettings = vm::updateNotificationSettings,
                                    onLogout = vm::logout,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
