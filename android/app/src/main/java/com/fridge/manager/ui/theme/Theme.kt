package com.fridge.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF0F172A),
    background = Color(0xFFF5F7FB),
    surface = Color.White,
    error = Color(0xFFDC2626),
)

@Composable
fun FridgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
