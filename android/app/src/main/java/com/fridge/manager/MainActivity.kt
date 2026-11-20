package com.fridge.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fridge.manager.core.notification.NotificationHelper
import com.fridge.manager.ui.main.MainViewModel
import com.fridge.manager.ui.screens.AuthOnboardingScreen
import com.fridge.manager.ui.screens.FamilyAndSettingsScreen
import com.fridge.manager.ui.screens.InventoryScreen
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
                val nickname by vm.nickname.collectAsState()
                val locationsTree by vm.locationTree.collectAsState()
                val flatLocations by vm.flatLocations.collectAsState()
                val activeItems by vm.activeItems.collectAsState()
                val activities by vm.activities.collectAsState()
                val inviteCode by vm.inviteCode.collectAsState()
                val notificationSettings by vm.notificationSettings.collectAsState()
                val message by vm.message.collectAsState()

                val snackbarHostState = remember { SnackbarHostState() }
                var selectedTab by remember { mutableIntStateOf(0) }

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
                    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                            AuthOnboardingScreen(
                                isLoggedIn = !token.isNullOrBlank(),
                                onDevLogin = vm::devLogin,
                                onCreateHousehold = vm::createHousehold,
                                onJoinHousehold = vm::joinHousehold,
                            )
                        }
                    }
                } else {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Text("🥬") },
                                    label = { Text("재고·장소") },
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Text("👨‍👩‍👧‍👦") },
                                    label = { Text("가족·알림") },
                                )
                            }
                        },
                    ) { padding ->
                        androidx.compose.foundation.layout.Box(Modifier.padding(padding)) {
                            if (selectedTab == 0) {
                                InventoryScreen(
                                    locationsTree = locationsTree,
                                    flatLocations = flatLocations,
                                    items = activeItems,
                                    expiringOnlyInitial = openExpiring,
                                    onCreateLocation = vm::createLocation,
                                    onDeleteLocation = vm::deleteLocation,
                                    onCreateItem = vm::createItem,
                                    onAdjustQuantity = vm::adjustQuantity,
                                    onConsumeItem = vm::consumeItem,
                                    onDiscardItem = vm::discardItem,
                                )
                            } else {
                                FamilyAndSettingsScreen(
                                    nickname = nickname,
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
