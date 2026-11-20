package com.fridge.manager.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fridge.manager.core.datastore.NotificationSettings
import com.fridge.manager.domain.model.ActivityLogItem

@Composable
fun FamilyAndSettingsScreen(
    nickname: String?,
    inviteCode: String?,
    activities: List<ActivityLogItem>,
    notificationSettings: NotificationSettings,
    onGenerateInvite: () -> Unit,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("👨‍👩‍👧‍👦 가족 공유 (${nickname ?: ""})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onGenerateInvite) {
                        Text("초대 코드 발급")
                    }
                    if (!inviteCode.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("발급된 코드: $inviteCode", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("⏰ 로컬 알림 설정 (WorkManager)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("매일 오전 ${notificationSettings.hour}시 임박 알림")
                        Switch(
                            checked = notificationSettings.enabled,
                            onCheckedChange = {
                                onUpdateNotificationSettings(notificationSettings.copy(enabled = it))
                            },
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("만료된 품목도 알림 포함")
                        Switch(
                            checked = notificationSettings.includeExpired,
                            onCheckedChange = {
                                onUpdateNotificationSettings(
                                    notificationSettings.copy(includeExpired = it)
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            Text("🕒 최근 가족 변경 이력", style = MaterialTheme.typography.titleMedium)
        }

        items(activities, key = { it.id }) { log ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${log.actorNickname} · [${log.entityType}/${log.actionType}] ${log.summaryName ?: ""}",
                    modifier = Modifier.padding(12.dp),
                )
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("로그아웃")
            }
        }
    }
}
