package com.fridge.manager.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridge.manager.core.datastore.NotificationSettings
import com.fridge.manager.domain.model.ActivityLogItem
import com.fridge.manager.ui.theme.FreshKitchenColors
import com.fridge.manager.ui.theme.FreshKitchenMotion
import com.fridge.manager.ui.theme.FreshKitchenShapes

@Composable
fun FamilyAndSettingsScreen(
    nickname: String?,
    householdName: String,
    inviteCode: String?,
    activities: List<ActivityLogItem>,
    notificationSettings: NotificationSettings,
    onGenerateInvite: () -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onLogout: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val displayName = nickname?.takeIf { it.isNotBlank() } ?: "민수"
    var showTimeMenu by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val otherMembers = remember(activities, displayName) {
        activities.map { it.actorNickname }
            .filter { it.isNotBlank() && it != displayName }
            .distinct()
            .take(4)
    }
    val memberCount = 1 + otherMembers.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(FreshKitchenColors.CanvasBackground),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Clean Top App Bar
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
                            imageVector = Icons.Outlined.Kitchen,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = "가족·알림 설정",
                            style = MaterialTheme.typography.titleLarge,
                            color = FreshKitchenColors.TextPrimary,
                        )
                    }
                    TextButton(onClick = { showLogoutConfirm = true }) {
                        Text(
                            text = "로그아웃",
                            style = MaterialTheme.typography.labelLarge,
                            color = FreshKitchenColors.TextMuted,
                        )
                    }
                }
                HorizontalDivider(color = FreshKitchenColors.BorderSubtle)
            }
        }

        // Top Profile Header
        item {
            StitchCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(FreshKitchenColors.PrimarySoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = displayName.take(1),
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.PrimaryDark,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = FreshKitchenColors.TextPrimary,
                                )
                                Surface(
                                    shape = FreshKitchenShapes.Pill,
                                    color = FreshKitchenColors.PrimaryTealDeep,
                                ) {
                                    Text(
                                        text = "방장",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                text = householdName,
                                style = MaterialTheme.typography.bodySmall,
                                color = FreshKitchenColors.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        // Card 1: Household Members & Invite Code
        item {
            StitchCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
                                imageVector = Icons.Outlined.Group,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "우리집 구성원",
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.TextPrimary,
                            )
                            Surface(
                                shape = FreshKitchenShapes.Pill,
                                color = FreshKitchenColors.PrimarySoft,
                            ) {
                                Text(
                                    text = "${memberCount}명",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FreshKitchenColors.PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onGenerateInvite)
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAddAlt1,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "초대",
                                style = MaterialTheme.typography.labelLarge,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }

                    // Member Tiles Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MemberAvatarTile(
                            name = displayName,
                            role = "나 (관리자)",
                            isOwner = true,
                            isHighlighted = true,
                            modifier = Modifier.weight(1f),
                        )
                        otherMembers.take(2).forEach { member ->
                            MemberAvatarTile(
                                name = member,
                                role = "구성원",
                                isOwner = false,
                                isHighlighted = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    // Invite Code Sub-Card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FreshKitchenShapes.Control)
                            .background(FreshKitchenColors.SurfaceMuted)
                            .padding(14.dp),
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
                                    imageVector = Icons.Outlined.Key,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "우리집 초대 코드",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = FreshKitchenColors.TextPrimary,
                                )
                            }
                            Text(
                                text = "24시간 유효",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshKitchenColors.ExpiringText,
                            )
                        }

                        if (!inviteCode.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(FreshKitchenShapes.Control)
                                    .background(FreshKitchenColors.SurfaceWhite)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = inviteCode,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FreshKitchenColors.PrimaryTealDeep,
                                    letterSpacing = 2.sp,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Surface(
                                        onClick = { clipboardManager.setText(AnnotatedString(inviteCode)) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = FreshKitchenColors.PrimarySoft,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = null,
                                                tint = FreshKitchenColors.PrimaryDark,
                                                modifier = Modifier.size(14.dp),
                                            )
                                            Text(
                                                text = "복사",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = FreshKitchenColors.PrimaryDark,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "[$householdName] 우리집 냉장고 초대 코드: $inviteCode",
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "초대 코드 공유"))
                                        },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = "공유",
                                            tint = FreshKitchenColors.TextMuted,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onGenerateInvite)
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.TextMuted,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = if (inviteCode.isNullOrBlank()) "초대 코드 발급받기" else "새 코드 발급받기",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FreshKitchenColors.TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 2: Local Notification Settings
        item {
            StitchCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
                                imageVector = Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "유통기한 로컬 알림",
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Switch(
                            checked = notificationSettings.enabled,
                            onCheckedChange = {
                                onUpdateNotificationSettings(notificationSettings.copy(enabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = FreshKitchenColors.PrimaryTealDeep,
                            ),
                        )
                    }

                    AnimatedVisibility(
                        visible = notificationSettings.enabled,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = FreshKitchenMotion.StandardMs,
                                easing = FreshKitchenMotion.EnterEasing,
                            ),
                        ) + expandVertically(
                            animationSpec = tween(
                                durationMillis = FreshKitchenMotion.StandardMs,
                                easing = FreshKitchenMotion.EnterEasing,
                            ),
                        ),
                        exit = fadeOut(
                            animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
                        ) + shrinkVertically(
                            animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
                        ),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "알림 기준일",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FreshKitchenColors.TextPrimary,
                                    )
                                    Text(
                                        text = "기준일 선택",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FreshKitchenColors.TextMuted,
                                    )
                                }
                                Text(
                                    text = "만료 전 지정한 날짜에 기기 알림을 미리 전송합니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FreshKitchenColors.TextMuted,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(1, 3, 5, 7).forEach { days ->
                                        val selected = notificationSettings.alertDays == days
                                        Surface(
                                            onClick = {
                                                onUpdateNotificationSettings(
                                                    notificationSettings.copy(alertDays = days),
                                                )
                                            },
                                            shape = FreshKitchenShapes.Pill,
                                            color = if (selected) {
                                                FreshKitchenColors.PrimaryTealDeep
                                            } else {
                                                FreshKitchenColors.SurfaceMuted
                                            },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 9.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                if (selected) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(14.dp),
                                                    )
                                                    Spacer(Modifier.width(3.dp))
                                                }
                                                Text(
                                                    text = "D-$days",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (selected) Color.White else FreshKitchenColors.TextPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Notification Time Row with Dropdown Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "알림 시간",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FreshKitchenColors.TextPrimary,
                                    )
                                    Text(
                                        text = "식재료 확인에 가장 편한 시간",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FreshKitchenColors.TextMuted,
                                    )
                                }
                                Box {
                                    val timeLabel = when (notificationSettings.hour) {
                                        in 0..11 -> "오전 ${String.format("%02d:00", notificationSettings.hour)}"
                                        12 -> "오후 12:00"
                                        else -> "오후 ${String.format("%02d:00", notificationSettings.hour - 12)}"
                                    }
                                    Surface(
                                        onClick = { showTimeMenu = true },
                                        shape = FreshKitchenShapes.Pill,
                                        color = FreshKitchenColors.SurfaceMuted,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Schedule,
                                                contentDescription = null,
                                                tint = FreshKitchenColors.PrimaryTealDeep,
                                                modifier = Modifier.size(16.dp),
                                            )
                                            Text(
                                                text = timeLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = FreshKitchenColors.TextPrimary,
                                            )
                                            Icon(
                                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = FreshKitchenColors.TextMuted,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showTimeMenu,
                                        onDismissRequest = { showTimeMenu = false },
                                    ) {
                                        listOf(
                                            8 to "오전 08:00",
                                            9 to "오전 09:00",
                                            12 to "오후 12:00",
                                            18 to "오후 06:00",
                                            20 to "오후 08:00",
                                        ).forEach { (hr, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                onClick = {
                                                    showTimeMenu = false
                                                    onUpdateNotificationSettings(
                                                        notificationSettings.copy(hour = hr, minute = 0),
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "만료 품목 당일 재알림",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = FreshKitchenColors.TextPrimary,
                                    )
                                    Text(
                                        text = "소비 기한이 지난 식재료도 함께 안내합니다",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FreshKitchenColors.TextMuted,
                                    )
                                }
                                Switch(
                                    checked = notificationSettings.includeExpired,
                                    onCheckedChange = {
                                        onUpdateNotificationSettings(
                                            notificationSettings.copy(includeExpired = it),
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = FreshKitchenColors.PrimaryTealDeep,
                                    ),
                                )
                            }

                            // Shield Info Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(FreshKitchenShapes.Control)
                                    .background(FreshKitchenColors.SurfaceMuted)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.VerifiedUser,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "기기 로컬 알림으로 서버 장애나 백그라운드 끊김 없이 정확하게 정시에 울립니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = FreshKitchenColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Recent Activity History Timeline
        item {
            StitchCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
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
                                imageVector = Icons.Outlined.History,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "최근 변경 이력",
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                    }

                    if (activities.isEmpty()) {
                        Text(
                            text = "아직 기록된 변경 이력이 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = FreshKitchenColors.TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column {
                            activities.forEachIndexed { idx, log ->
                                ActivityTimelineRow(
                                    log = log,
                                    isLast = idx == activities.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = {
                Text(
                    text = "로그아웃할까요?",
                    style = MaterialTheme.typography.titleMedium,
                    color = FreshKitchenColors.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "로그아웃하면 기기에 저장된 로그인 세션과 로컬 알림 예약이 해제됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = FreshKitchenColors.TextSecondary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    shape = FreshKitchenShapes.Control,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FreshKitchenColors.ExpiredText,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("로그아웃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("취소", color = FreshKitchenColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun StitchCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FreshKitchenShapes.Card,
        color = FreshKitchenColors.SurfaceWhite,
        border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
        content = content,
    )
}

@Composable
private fun MemberAvatarTile(
    name: String,
    role: String,
    isOwner: Boolean,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(FreshKitchenShapes.Control)
            .background(FreshKitchenColors.SurfaceMuted)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHighlighted) FreshKitchenColors.PrimaryTealDeep else FreshKitchenColors.NeutralTrack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.take(1).ifBlank { "?" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isHighlighted) Color.White else FreshKitchenColors.TextPrimary,
                )
            }
            if (isOwner) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = FreshKitchenColors.PrimaryTealDeep,
                    border = BorderStroke(1.5.dp, Color.White),
                ) {
                    Text(
                        text = "방장",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = FreshKitchenColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = role,
            style = MaterialTheme.typography.labelSmall,
            color = FreshKitchenColors.TextMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ActivityTimelineRow(
    log: ActivityLogItem,
    isLast: Boolean,
) {
    val (badgeLabel, badgeBg, badgeText) = when (log.actionType.uppercase()) {
        "CREATED" -> Triple("등록", FreshKitchenColors.PrimarySoft, FreshKitchenColors.PrimaryDark)
        "CONSUMED" -> Triple("소비", FreshKitchenColors.SafeBg, FreshKitchenColors.SafeText)
        "DISCARDED", "DELETED" -> Triple("폐기", FreshKitchenColors.ExpiredBg, FreshKitchenColors.ExpiredText)
        "MOVED" -> Triple("이동", FreshKitchenColors.ExpiringBg, FreshKitchenColors.ExpiringText)
        else -> Triple(log.actionType, FreshKitchenColors.NoneBg, FreshKitchenColors.NoneText)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Timeline dot + vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FreshKitchenColors.PrimaryTealDeep),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = log.actorNickname.take(1).ifBlank { "?" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(FreshKitchenColors.BorderSubtle),
                )
            }
        }

        // Card content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FreshKitchenColors.CanvasBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = badgeBg,
                ) {
                    Text(
                        text = badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = log.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = FreshKitchenColors.TextMuted,
                )
            }
            Text(
                text = "${log.actorNickname}님이 ${log.summaryName ?: "${log.entityType} ${log.actionType}"}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = FreshKitchenColors.TextPrimary,
                lineHeight = 18.sp,
            )
        }
    }
}
