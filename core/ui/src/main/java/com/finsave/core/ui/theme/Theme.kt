package com.finsave.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * FinSave Theme — Material 3 with FinSave brand identity.
 *
 * Features:
 * - Dynamic color support (Material You on Android 12+)
 * - OLED-optimized dark mode (#111928 background)
 * - Indigo primary with Teal secondary
 * - Spacing provided via CompositionLocal
 * - Full light/dark variant coverage
 */

// ── Light Color Scheme ─────────────────────────────────────────────────────
private val FinSaveLightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,

    secondary = Teal600,
    onSecondary = Color.White,
    secondaryContainer = Teal100,
    onSecondaryContainer = Teal700,

    tertiary = Amber600,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber700,

    error = DebitRed,
    onError = Color.White,
    errorContainer = DebitRedLight,
    onErrorContainer = DangerRed,

    background = SurfaceLight,
    onBackground = OnSurfaceLight,

    surface = SurfaceContainerLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,

    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB),
    inversePrimary = Indigo300,
    surfaceTint = Indigo600
)

// ── Dark Color Scheme (OLED Optimized) ─────────────────────────────────────
private val FinSaveDarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Indigo900,
    primaryContainer = Indigo800,
    onPrimaryContainer = Indigo100,

    secondary = Teal400,
    onSecondary = Teal700,
    secondaryContainer = Teal700,
    onSecondaryContainer = Teal100,

    tertiary = Amber400,
    onTertiary = Amber700,
    tertiaryContainer = Amber700,
    onTertiaryContainer = Amber100,

    error = DebitRedDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = SurfaceDark,
    onBackground = OnSurfaceDark,

    surface = SurfaceContainerDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,

    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151),
    inverseSurface = Color(0xFFF9FAFB),
    inverseOnSurface = Color(0xFF1F2937),
    inversePrimary = Indigo700,
    surfaceTint = Indigo400
)

/**
 * FinSave App Theme.
 *
 * @param darkTheme Whether to use dark mode (defaults to system setting)
 * @param dynamicColor Whether to use Material You dynamic colors (Android 12+)
 * @param content The composable content to theme
 */
@Composable
fun FinSaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Off by default — FinSave has strong brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> FinSaveDarkColorScheme
        else -> FinSaveLightColorScheme
    }

    CompositionLocalProvider(
        LocalSpacing provides Spacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinSaveTypography,
            shapes = FinSaveShapes,
            content = content
        )
    }
}
