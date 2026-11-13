package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AgriculturalLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = WheatGold,
    onSecondary = Color.White,
    secondaryContainer = WheatGoldLight,
    onSecondaryContainer = WheatGoldDark,
    tertiary = StatusSettlementPurple,
    onTertiary = Color.White,
    background = WheatCreamBg,
    onBackground = TextPrimary,
    surface = WheatSurface,
    onSurface = TextPrimary,
    surfaceVariant = WheatMutedSurface,
    onSurfaceVariant = TextSecondary,
    outline = WheatCardBorder,
    error = StatusUnpaidRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AgriculturalLightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
