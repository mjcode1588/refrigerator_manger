package com.fridge.manager.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object FreshKitchenColors {
    // Primary Teal Palette
    val PrimaryTealDeep = Color(0xFF00685F)
    val PrimaryTeal = Color(0xFF0D9488)
    val PrimaryDark = Color(0xFF005049)
    val PrimarySoft = Color(0xFFCCFBF1)
    val PrimaryMint = Color(0xFF9CF2E8)
    val MintGlow = Color(0xFFF0FDFA)
    val MintBorder = Color(0xFF99F6E4)
    val TipBannerBg = Color(0xFFE6F4F1)

    // Surfaces & Text
    val CanvasBackground = Color(0xFFF8FAFC)
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFEFF4F8)
    val SurfaceSubtle = Color(0xFFF1F5F9)
    val BorderSubtle = Color(0xFFE2E8F0)
    val NeutralTrack = Color(0xFFCBD5E1)
    val TextPrimary = Color(0xFF0B1C30)
    val TextSecondary = Color(0xFF3D4947)
    val TextMuted = Color(0xFF64748B)

    // D-Day Semantic Badges
    val ExpiredBg = Color(0xFFFEE2E2)
    val ExpiredText = Color(0xFFDC2626)
    val TodayBg = Color(0xFFFFEDD5)
    val TodayText = Color(0xFFEA580C)
    val ExpiringBg = Color(0xFFFEF3C7)
    val ExpiringText = Color(0xFFD97706)
    val SafeBg = Color(0xFFDCFCE7)
    val SafeText = Color(0xFF16A34A)
    val NoneBg = Color(0xFFF1F5F9)
    val NoneText = Color(0xFF64748B)
}

object FreshKitchenShapes {
    val Card = RoundedCornerShape(16.dp)
    val Control = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(999.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

object FreshKitchenMotion {
    const val FastMs = 150
    const val StandardMs = 200
    val EnterEasing: Easing = FastOutSlowInEasing
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

private val BaseTypography = Typography()

private val FreshTypography = BaseTypography.copy(
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    titleLarge = BaseTypography.titleLarge.copy(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    titleMedium = BaseTypography.titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        fontFeatureSettings = "tnum",
    ),
    titleSmall = BaseTypography.titleSmall.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = "tnum",
    ),
    bodySmall = BaseTypography.bodySmall.copy(
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        fontFeatureSettings = "tnum",
    ),
    labelLarge = BaseTypography.labelLarge.copy(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    labelMedium = BaseTypography.labelMedium.copy(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
    labelSmall = BaseTypography.labelSmall.copy(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    ),
)

private val FreshShapes = Shapes(
    small = FreshKitchenShapes.Control,
    medium = FreshKitchenShapes.Card,
    large = FreshKitchenShapes.Card,
)

@Composable
fun FridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = FreshTypography,
        shapes = FreshShapes,
        content = content,
    )
}
