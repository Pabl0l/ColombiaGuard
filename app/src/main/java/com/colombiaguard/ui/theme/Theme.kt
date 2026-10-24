package com.colombiaguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ColombiaGuard Theme - Professional Dark-First Design
 *
 * Always dark. Security apps should feel solid and trustworthy.
 * No dynamic color (wallpaper-based) - consistency across devices.
 */

private val DarkColorScheme = darkColorScheme(
    // Background
    background = Background,
    onBackground = OnBackground,

    // Surface
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Color.Transparent,

    // Primary (blue)
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryMuted,
    onPrimaryContainer = OnPrimary,

    // Secondary
    secondary = Secondary,
    onSecondary = OnSecondary,

    // Tertiary
    tertiary = Tertiary,
    onTertiary = OnTertiary,

    // Error / Blocked
    error = Error,
    onError = Color.White,
    errorContainer = ErrorMuted,
    onErrorContainer = Color.White,

    // Borders / Outlines
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,

    // Inverse
    inverseSurface = OnSurface,
    inverseOnSurface = Surface,
    inversePrimary = PrimaryDark,
)

@Composable
fun ColombiaGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Always use dark scheme - security apps don't need light mode
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
