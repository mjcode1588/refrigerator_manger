package com.fridge.manager.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fridge.manager.BuildConfig
import com.fridge.manager.ui.theme.FreshKitchenColors

@Composable
fun AuthOnboardingScreen(
    isLoggedIn: Boolean,
    isLoggingIn: Boolean = false,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (username: String, password: String, nickname: String, registrationCode: String) -> Unit,
    onDevLogin: (String) -> Unit,
    onCreateHousehold: (String) -> Unit,
    onJoinHousehold: (String) -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var authTab by remember { mutableIntStateOf(0) } // 0: 로그인, 1: 가족 등록
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var registrationCode by remember { mutableStateOf("") }
    var registrationCodeVisible by remember { mutableStateOf(false) }

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
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 2단 세그먼트 탭: 로그인 / 가족 등록
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FreshKitchenColors.SurfaceMuted)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AuthSegmentTab(
                            label = "로그인",
                            selected = authTab == 0,
                            onClick = { if (!isLoggingIn) authTab = 0 },
                            modifier = Modifier.weight(1f),
                        )
                        AuthSegmentTab(
                            label = "가족 등록",
                            selected = authTab == 1,
                            onClick = { if (!isLoggingIn) authTab = 1 },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    if (authTab == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "아이디",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "영문·숫자 아이디 입력",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "비밀번호",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "비밀번호 입력 (8자 이상)",
                                leadingIcon = Icons.Rounded.Lock,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        val canLogin = !isLoggingIn && username.isNotBlank() && password.isNotBlank()
                        Button(
                            onClick = { onLogin(username.trim(), password) },
                            enabled = canLogin,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshKitchenColors.PrimaryTealDeep,
                                contentColor = Color.White,
                                disabledContainerColor = FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("서버에 연결 중입니다...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("로그인", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "닉네임",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                placeholder = "예: 남편, 아내",
                                leadingIcon = Icons.Rounded.Badge,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "아이디",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "영문·숫자·_·- (3~32자)",
                                leadingIcon = Icons.Rounded.Person,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "비밀번호",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "8자 이상 비밀번호",
                                leadingIcon = Icons.Rounded.Lock,
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "가족 등록코드",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = registrationCode,
                                onValueChange = { registrationCode = it },
                                placeholder = "서버에 설정된 가족 등록코드",
                                leadingIcon = Icons.Rounded.Key,
                                isPassword = true,
                                passwordVisible = registrationCodeVisible,
                                onTogglePasswordVisibility = {
                                    registrationCodeVisible = !registrationCodeVisible
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        val canRegister = !isLoggingIn &&
                            nickname.isNotBlank() &&
                            username.trim().length >= 3 &&
                            password.length >= 8 &&
                            registrationCode.isNotBlank()
                        Button(
                            onClick = {
                                onRegister(
                                    username.trim(),
                                    password,
                                    nickname.trim(),
                                    registrationCode.trim(),
                                )
                            },
                            enabled = canRegister,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshKitchenColors.PrimaryTealDeep,
                                contentColor = Color.White,
                                disabledContainerColor = FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("서버에 연결 중입니다...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Text("우리집 계정 등록하기", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 가족 전용 보안 배지
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(FreshKitchenColors.MintGlow)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "우리집 가족 전용 · 2인 등록 완료 시 외부 가입 자동 차단",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = FreshKitchenColors.PrimaryDark,
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(18.dp))

                // Preview demo account pill (Debug 빌드 전용)
                Surface(
                    onClick = { onDevLogin("민수") },
                    enabled = !isLoggingIn,
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
                            text = "체험 계정으로 먼저 둘러보기 (Debug)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = FreshKitchenColors.PrimaryDark,
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                text = "로그인 세션은 30일간 안전하게 암호화되어 유지됩니다.",
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
private fun AuthSegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) FreshKitchenColors.PrimaryTealDeep else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else FreshKitchenColors.TextMuted,
        )
    }
}

@Composable
private fun SoftInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FreshKitchenColors.SurfaceMuted)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = FreshKitchenColors.TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = FreshKitchenColors.TextMuted,
                    fontSize = 14.sp,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                textStyle = TextStyle(
                    color = FreshKitchenColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(FreshKitchenColors.PrimaryTealDeep),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (isPassword && onTogglePasswordVisibility != null) {
            IconButton(
                onClick = onTogglePasswordVisibility,
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = if (passwordVisible) {
                        Icons.Rounded.VisibilityOff
                    } else {
                        Icons.Rounded.Visibility
                    },
                    contentDescription = if (passwordVisible) "숨기기" else "표시",
                    tint = FreshKitchenColors.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else if (value.isNotEmpty()) {
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
