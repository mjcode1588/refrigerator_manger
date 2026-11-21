package com.fridge.manager.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import com.fridge.manager.ui.theme.FreshKitchenColors

@Composable
fun AuthOnboardingScreen(
    isLoggedIn: Boolean,
    isLoggingIn: Boolean = false,
    onDevLogin: (String) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var nickname by remember { mutableStateOf("") }
    var showCustomNickname by remember { mutableStateOf(false) }
    var householdName by remember { mutableStateOf("행복한 우리집") }
    var createDefaultLocations by remember { mutableStateOf(true) }
    var inviteCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FreshKitchenColors.CanvasBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        // Stitch Hero Icon with floating leaf & peach accents
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(FreshKitchenColors.PrimaryMint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Kitchen,
                    contentDescription = null,
                    tint = FreshKitchenColors.PrimaryTealDeep,
                    modifier = Modifier.size(38.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6EE7B7)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = FreshKitchenColors.PrimaryDark,
                    modifier = Modifier.size(16.dp),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(FreshKitchenColors.PeachSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = null,
                    tint = FreshKitchenColors.TextPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "우리집 냉장고",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FreshKitchenColors.TextPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "가족과 함께 놓치지 않는 식재료·유통기한 관리",
            style = MaterialTheme.typography.bodyMedium,
            color = FreshKitchenColors.TextMuted,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        if (!isLoggedIn) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.06f),
                    ),
                shape = RoundedCornerShape(24.dp),
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = FreshKitchenColors.BorderSubtle,
                        )
                        Text(
                            text = "간편 로그인으로 시작하기",
                            style = MaterialTheme.typography.labelMedium,
                            color = FreshKitchenColors.TextMuted,
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = FreshKitchenColors.BorderSubtle,
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // Kakao CTA (카카오 SDK 연동 전까지 클릭 시 무반응)
                    Button(
                        onClick = {},
                        enabled = !isLoggingIn,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshKitchenColors.KakaoYellow,
                            contentColor = FreshKitchenColors.KakaoText,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = FreshKitchenColors.KakaoText,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("서버에 연결 중입니다...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubble,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("카카오로 시작하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Optional custom nickname for dev testing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TextButton(
                            onClick = { showCustomNickname = !showCustomNickname },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = if (showCustomNickname) "직접 입력 닫기" else "닉네임 직접 입력하기",
                                style = MaterialTheme.typography.labelMedium,
                                color = FreshKitchenColors.PrimaryTealDeep,
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCustomNickname) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SoftInputField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                placeholder = "닉네임 (예: 민수, 지영)",
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { if (nickname.isNotBlank()) onDevLogin(nickname.trim()) },
                                enabled = nickname.isNotBlank(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FreshKitchenColors.PrimaryTealDeep,
                                ),
                                modifier = Modifier.height(50.dp),
                            ) {
                                Text("시작", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // Preview demo account pill
            Surface(
                onClick = { onDevLogin("민수") },
                shape = RoundedCornerShape(999.dp),
                color = FreshKitchenColors.PrimarySoft,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircleOutline,
                        contentDescription = null,
                        tint = FreshKitchenColors.PrimaryDark,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "체험 계정으로 먼저 둘러보기",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = FreshKitchenColors.PrimaryDark,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "시작 시 이용약관 및 개인정보처리방침에 동의하게 됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = FreshKitchenColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "우리집 공간을 시작해볼까요?",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FreshKitchenColors.TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "새로운 냉장고 공간을 만들거나 이미 생성된 가족 공간에 합류하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FreshKitchenColors.TextMuted,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Option A: Create Household (with subtle mint glow)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f),
                    ),
                shape = RoundedCornerShape(24.dp),
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle.copy(alpha = 0.6f)),
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            colors = listOf(
                                FreshKitchenColors.SurfaceWhite,
                                FreshKitchenColors.MintGlow,
                            ),
                        ),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = "새 우리집 만들기",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = FreshKitchenColors.TextPrimary,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = FreshKitchenColors.PrimaryMint,
                            ) {
                                Text(
                                    text = "추천",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FreshKitchenColors.PrimaryDark,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Text(
                            text = "우리집 별칭",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = FreshKitchenColors.TextSecondary,
                        )

                        SoftInputField(
                            value = householdName,
                            onValueChange = { householdName = it },
                            placeholder = "예: 행복한 우리집",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(FreshKitchenColors.SurfaceMuted)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Kitchen,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.PrimaryTealDeep,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column {
                                    Text(
                                        text = "기본 보관 장소 자동 생성",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = FreshKitchenColors.TextPrimary,
                                    )
                                    Text(
                                        text = "냉장실 · 냉동실 · 실온 팬트리",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = FreshKitchenColors.TextMuted,
                                    )
                                }
                            }
                            Switch(
                                checked = createDefaultLocations,
                                onCheckedChange = { createDefaultLocations = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = FreshKitchenColors.PrimaryTealDeep,
                                ),
                            )
                        }

                        Button(
                            onClick = { if (householdName.isNotBlank()) onCreateHousehold(householdName.trim()) },
                            enabled = householdName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshKitchenColors.PrimaryTealDeep,
                                contentColor = Color.White,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            Text("냉장고 공간 생성하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Option B: Join via Invite Code
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.05f),
                    ),
                shape = RoundedCornerShape(24.dp),
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = null,
                            tint = FreshKitchenColors.TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "초대 코드로 참여하기",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = FreshKitchenColors.TextPrimary,
                        )
                    }
                    Text(
                        text = "가족이나 룸메이트에게 공유받은 8자리 코드를 입력해주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = FreshKitchenColors.TextMuted,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FreshKitchenColors.SurfaceMuted)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (inviteCode.isEmpty()) {
                                Text(
                                    text = "A 7 K 9 M 2 P Q",
                                    color = FreshKitchenColors.TextMuted.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 15.sp,
                                )
                            }
                            BasicTextField(
                                value = inviteCode,
                                onValueChange = { inviteCode = it.uppercase().take(12) },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = FreshKitchenColors.TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center,
                                ),
                                cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Surface(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text?.trim().orEmpty()
                                if (pasted.isNotEmpty()) {
                                    inviteCode = pasted.uppercase().take(12)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE2E8F0),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.TextSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "붙여넣기",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = FreshKitchenColors.TextSecondary,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { if (inviteCode.isNotBlank()) onJoinHousehold(inviteCode.trim()) },
                        enabled = inviteCode.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (inviteCode.isNotBlank()) {
                                FreshKitchenColors.PrimaryTealDeep
                            } else {
                                FreshKitchenColors.SurfaceMuted
                            },
                            contentColor = if (inviteCode.isNotBlank()) {
                                Color.White
                            } else {
                                FreshKitchenColors.TextPrimary
                            },
                            disabledContainerColor = FreshKitchenColors.SurfaceMuted,
                            disabledContentColor = FreshKitchenColors.TextMuted,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("가족 공간 참여하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FreshKitchenColors.SurfaceMuted)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = FreshKitchenColors.TextMuted,
                    fontSize = 15.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = FreshKitchenColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cancel,
                    contentDescription = "지우기",
                    tint = FreshKitchenColors.TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
