package com.fridge.manager.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import com.fridge.manager.ui.theme.FreshKitchenMotion
import com.fridge.manager.ui.theme.FreshKitchenShapes

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
        Spacer(Modifier.height(16.dp))

        // Refined minimal brand emblem
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(18.dp),
            color = FreshKitchenColors.PrimaryTealDeep,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Kitchen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "우리집 냉장고",
            style = MaterialTheme.typography.headlineMedium,
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
                modifier = Modifier.fillMaxWidth(),
                shape = FreshKitchenShapes.Card,
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // 2단 세그먼트 탭: 로그인 / 가족 등록
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FreshKitchenShapes.Control)
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

                    AnimatedContent(
                        targetState = authTab,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (fadeIn(
                                animationSpec = tween(
                                    durationMillis = FreshKitchenMotion.StandardMs,
                                    easing = FreshKitchenMotion.EnterEasing,
                                ),
                            ) + slideInHorizontally(
                                animationSpec = tween(
                                    durationMillis = FreshKitchenMotion.StandardMs,
                                    easing = FreshKitchenMotion.EnterEasing,
                                ),
                                initialOffsetX = { it / 16 * dir },
                            )) togetherWith (fadeOut(
                                animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
                            ) + slideOutHorizontally(
                                animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
                                targetOffsetX = { -it / 16 * dir },
                            )) using SizeTransform(clip = false)
                        },
                        label = "auth_tab_transition",
                    ) { currentTab ->
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (currentTab == 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "아이디",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = FreshKitchenColors.TextSecondary,
                                    )
                            SoftInputField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "영문·숫자 아이디 입력",
                                leadingIcon = Icons.Outlined.Person,
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
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "비밀번호 입력 (8자 이상)",
                                leadingIcon = Icons.Outlined.Lock,
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
                            shape = FreshKitchenShapes.Control,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshKitchenColors.PrimaryTealDeep,
                                contentColor = Color.White,
                                disabledContainerColor = FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("서버에 연결 중입니다...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Text("로그인", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Login,
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
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = nickname,
                                onValueChange = { nickname = it },
                                placeholder = "예: 남편, 아내",
                                leadingIcon = Icons.Outlined.Badge,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "아이디",
                                style = MaterialTheme.typography.labelMedium,
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = "영문·숫자·_·- (3~32자)",
                                leadingIcon = Icons.Outlined.Person,
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
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = "8자 이상 비밀번호",
                                leadingIcon = Icons.Outlined.Lock,
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
                                color = FreshKitchenColors.TextSecondary,
                            )
                            SoftInputField(
                                value = registrationCode,
                                onValueChange = { registrationCode = it },
                                placeholder = "서버에 설정된 가족 등록코드",
                                leadingIcon = Icons.Outlined.Key,
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
                            shape = FreshKitchenShapes.Control,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FreshKitchenColors.PrimaryTealDeep,
                                contentColor = Color.White,
                                disabledContainerColor = FreshKitchenColors.PrimaryTealDeep.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            if (isLoggingIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("서버에 연결 중입니다...", style = MaterialTheme.typography.labelLarge)
                            } else {
                                Text("우리집 계정 등록하기", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                        }
                    }

                    // 가족 전용 보안 배지
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(FreshKitchenShapes.Control)
                            .background(FreshKitchenColors.MintGlow)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryTealDeep,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "우리집 가족 전용 · 2인 등록 완료 시 외부 가입 자동 차단",
                            style = MaterialTheme.typography.labelSmall,
                            color = FreshKitchenColors.PrimaryDark,
                        )
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(16.dp))

                // Preview demo account pill (Debug 빌드 전용)
                Surface(
                    onClick = { onDevLogin("민수") },
                    enabled = !isLoggingIn,
                    shape = FreshKitchenShapes.Pill,
                    color = FreshKitchenColors.PrimarySoft,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircleOutline,
                            contentDescription = null,
                            tint = FreshKitchenColors.PrimaryDark,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "체험 계정으로 먼저 둘러보기 (Debug)",
                            style = MaterialTheme.typography.labelLarge,
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
                    style = MaterialTheme.typography.titleLarge,
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

            // Option A: Create Household (flat surface, no gradient)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = FreshKitchenShapes.Card,
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
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
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null,
                                tint = FreshKitchenColors.PrimaryTealDeep,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "새 우리집 만들기",
                                style = MaterialTheme.typography.titleMedium,
                                color = FreshKitchenColors.TextPrimary,
                            )
                        }
                        Surface(
                            shape = FreshKitchenShapes.Pill,
                            color = FreshKitchenColors.PrimarySoft,
                        ) {
                            Text(
                                text = "추천",
                                style = MaterialTheme.typography.labelSmall,
                                color = FreshKitchenColors.PrimaryDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Text(
                        text = "우리집 별칭",
                        style = MaterialTheme.typography.labelMedium,
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
                            .clip(FreshKitchenShapes.Control)
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
                                imageVector = Icons.Outlined.Kitchen,
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
                        shape = FreshKitchenShapes.Control,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FreshKitchenColors.PrimaryTealDeep,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                    ) {
                        Text("냉장고 공간 생성하기", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Option B: Join via Invite Code
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = FreshKitchenShapes.Card,
                color = FreshKitchenColors.SurfaceWhite,
                border = BorderStroke(1.dp, FreshKitchenColors.BorderSubtle),
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
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = FreshKitchenColors.TextMuted,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "초대 코드로 참여하기",
                            style = MaterialTheme.typography.titleMedium,
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
                            .clip(FreshKitchenShapes.Control)
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
                                    fontWeight = FontWeight.Bold,
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
                            color = FreshKitchenColors.BorderSubtle,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    tint = FreshKitchenColors.TextSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "붙여넣기",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = FreshKitchenColors.TextSecondary,
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { if (inviteCode.isNotBlank()) onJoinHousehold(inviteCode.trim()) },
                        enabled = inviteCode.isNotBlank(),
                        shape = FreshKitchenShapes.Control,
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
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("가족 공간 참여하기", style = MaterialTheme.typography.labelLarge)
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
    val bgColor by animateColorAsState(
        targetValue = if (selected) FreshKitchenColors.PrimaryTealDeep else Color.Transparent,
        animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
        label = "segment_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else FreshKitchenColors.TextMuted,
        animationSpec = tween(durationMillis = FreshKitchenMotion.FastMs),
        label = "segment_content",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
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
            .height(48.dp)
            .clip(FreshKitchenShapes.Control)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = FreshKitchenColors.TextMuted,
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
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
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
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = "지우기",
                    tint = FreshKitchenColors.TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
