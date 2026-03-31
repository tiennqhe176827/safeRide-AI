package com.example.saferideai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Mint,
    onPrimary = DeepNavy,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = SoftWhite,
    secondary = ColorPalette.secondaryDark,
    onSecondary = SoftWhite,
    secondaryContainer = CardDark,
    onSecondaryContainer = SoftWhite,
    background = SurfaceDark,
    onBackground = SoftWhite,
    surface = CardDark,
    onSurface = SoftWhite,
    surfaceVariant = DeepNavy,
    onSurfaceVariant = ColorPalette.onSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = SoftWhite,
    primaryContainer = Mint,
    onPrimaryContainer = DeepNavy,
    secondary = ColorPalette.secondaryLight,
    onSecondary = SoftWhite,
    secondaryContainer = ColorPalette.secondaryContainerLight,
    onSecondaryContainer = DeepNavy,
    background = SurfaceLight,
    onBackground = DeepNavy,
    surface = SoftWhite,
    onSurface = DeepNavy,
    surfaceVariant = ColorPalette.surfaceVariantLight,
    onSurfaceVariant = Slate
)

@Composable
fun SafeRideAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private object ColorPalette {
    val secondaryLight = Color(0xFF1D4ED8)
    val secondaryContainerLight = Color(0xFFDCE8FF)
    val surfaceVariantLight = Color(0xFFEAF4EF)
    val secondaryDark = Color(0xFF60A5FA)
    val onSurfaceVariantDark = Color(0xFFCBD5E1)
}
