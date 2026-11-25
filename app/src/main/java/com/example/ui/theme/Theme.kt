package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DexDictateDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = MidnightNavy,
    primaryContainer = DexterBadgeDeepBlue,
    onPrimaryContainer = IceWhite,
    secondary = DexterBadgeBlue,
    onSecondary = IceWhite,
    secondaryContainer = ElevatedNavySurface,
    onSecondaryContainer = IceWhite,
    tertiary = AmberAlert,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = IceWhite,
    surface = DeepNavySurface,
    onSurface = IceWhite,
    surfaceVariant = ElevatedNavySurface,
    onSurfaceVariant = MutedSlate,
    outline = SubtleBorder,
    outlineVariant = CyanMuted,
    error = CoralError,
    onError = MidnightNavy
)

@Composable
fun DexDictateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DexDictateDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    DexDictateTheme(content = content)
}
