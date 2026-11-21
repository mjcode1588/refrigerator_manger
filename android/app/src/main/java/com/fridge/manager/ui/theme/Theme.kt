package com.fridge.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

object FreshKitchenColors {
    // Primary Stitch Teal Palette
    val PrimaryTealDeep = Color(0xFF00685F)
    val PrimaryTeal = Color(0xFF0D9488)
    val PrimaryDark = Color(0xFF005049)
    val PrimarySoft = Color(0xFFCCFBF1)
    val PrimaryMint = Color(0xFF9CF2E8)
    val MintGlow = Color(0xFFF0FDFA)
    val MintBorder = Color(0xFF99F6E4)

    // Surfaces & Text
    val CanvasBackground = Color(0xFFF8FAFC)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEFF4F8)
    val SurfaceSubtle = Color(0xFFF1F5F9)
    val BorderSubtle = Color(0xFFE2E8F0)
    val TextPrimary = Color(0xFF0B1C30)
    val TextSecondary = Color(0xFF3D4947)
    val TextMuted = Color(0xFF64748B)

    // D-Day Semantic Badges
    val ExpiredBg = Color(0xFFFEE2E2)
    val ExpiredText = Color(0xFFDC2626)
    val TodayBg = Color(0xFFFFEDD5)
    val TodayText = Color(0xFFEA580C)
    val ExpiringBg = Color(0xFFFEF7CD)
    val ExpiringText = Color(0xFFD97706)
    val SafeBg = Color(0xFFDCFCE7)
    val SafeText = Color(0xFF16A34A)
    val NoneBg = Color(0xFFF1F5F9)
    val NoneText = Color(0xFF64748B)

    // Brand Accents
    val KakaoYellow = Color(0xFFFEE500)
    val KakaoText = Color(0xFF191919)
    val PeachSoft = Color(0xFFFED7AA)
}

private val LightColors = lightColorScheme(
    primary = FreshKitchenColors.PrimaryTealDeep,
    onPrimary = Color.White,
    primaryContainer = FreshKitchenColors.PrimarySoft,
    onPrimaryContainer = FreshKitchenColors.PrimaryDark,
    secondary = FreshKitchenColors.PrimaryTeal,
    onSecondary = Color.White,
    background = FreshKitchenColors.CanvasBackground,
    onBackground = FreshKitchenColors.TextPrimary,
    surface = FreshKitchenColors.SurfaceWhite,
    onSurface = FreshKitchenColors.TextPrimary,
    surfaceVariant = FreshKitchenColors.SurfaceMuted,
    onSurfaceVariant = FreshKitchenColors.TextMuted,
    outline = FreshKitchenColors.BorderSubtle,
    error = FreshKitchenColors.ExpiredText,
)

private val FreshTypography = Typography(
    headlineMedium = TextStyle(fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(fontFeatureSettings = "tnum"),
    titleMedium = TextStyle(fontFeatureSettings = "tnum"),
    titleSmall = TextStyle(fontFeatureSettings = "tnum"),
    labelLarge = TextStyle(fontFeatureSettings = "tnum"),
    labelMedium = TextStyle(fontFeatureSettings = "tnum"),
    labelSmall = TextStyle(fontFeatureSettings = "tnum"),
)

@Composable
fun FridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = FreshTypography,
        content = content,
    )
}
