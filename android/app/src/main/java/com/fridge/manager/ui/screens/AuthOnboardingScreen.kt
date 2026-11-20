package com.fridge.manager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthOnboardingScreen(
    isLoggedIn: Boolean,
    onDevLogin: (String) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
) {
    var nickname by remember { mutableStateOf("") }
    var householdName by remember { mutableStateOf("우리집") }
    var inviteCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🧊 우리집 식재료 관리", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        if (!isLoggedIn) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("로그인", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("닉네임 (개발용 빠른 로그인)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { if (nickname.isNotBlank()) onDevLogin(nickname.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("닉네임으로 시작하기")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onDevLogin("민수") }, modifier = Modifier.weight(1f)) {
                            Text("민수")
                        }
                        OutlinedButton(onClick = { onDevLogin("지영") }, modifier = Modifier.weight(1f)) {
                            Text("지영")
                        }
                    }
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("우리집 시작하기", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = householdName,
                        onValueChange = { householdName = it },
                        label = { Text("새 가구 이름") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { if (householdName.isNotBlank()) onCreateHousehold(householdName.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("새 우리집 만들기")
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it },
                        label = { Text("초대 코드 입력") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { if (inviteCode.isNotBlank()) onJoinHousehold(inviteCode.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("초대 코드로 참여하기")
                    }
                }
            }
        }
    }
}
