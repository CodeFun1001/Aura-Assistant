package com.kastack.auraassistant.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val AuraPurple = Color(0xFF7C3AED)
val AuraPurpleLight = Color(0xFFA78BFA)
val AuraPurpleDark = Color(0xFF5B21B6)
val AuraGlow = Color(0xFFDDD6FE)
val AuraSurface = Color(0xFF0F0A1E)
val AuraSurfaceVariant = Color(0xFF1E1535)
val AuraOnSurface = Color(0xFFF5F3FF)

private val DarkColorScheme = darkColorScheme(
    primary = AuraPurpleLight,
    onPrimary = Color(0xFF1A0050),
    primaryContainer = AuraPurpleDark,
    onPrimaryContainer = AuraGlow,
    secondary = Color(0xFF9F87FF),
    onSecondary = Color(0xFF1A0050),
    background = AuraSurface,
    onBackground = AuraOnSurface,
    surface = AuraSurfaceVariant,
    onSurface = AuraOnSurface,
    surfaceVariant = Color(0xFF2D2145),
    onSurfaceVariant = Color(0xFFCCC2DC),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = AuraPurple,
    onPrimary = Color.White,
    primaryContainer = AuraGlow,
    onPrimaryContainer = AuraPurpleDark,
    secondary = Color(0xFF6D28D9),
    onSecondary = Color.White,
    background = Color(0xFFFAF5FF),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFEDE9FE),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun AuraAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}