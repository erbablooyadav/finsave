package com.finsave.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * FinSave Spacing System
 *
 * Consistent spacing throughout the app using a 4dp base grid.
 * Provided as a CompositionLocal so any composable can access it
 * without parameter drilling.
 *
 * Usage:
 * ```
 * val spacing = LocalSpacing.current
 * Modifier.padding(spacing.medium)
 * ```
 */
@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,     // 4dp  — Inner padding, icon gaps
    val small: Dp = 8.dp,          // 8dp  — Chip padding, tight lists
    val medium: Dp = 16.dp,        // 16dp — Card padding, section gaps
    val large: Dp = 24.dp,         // 24dp — Screen padding, major sections
    val extraLarge: Dp = 32.dp,    // 32dp — Onboarding screen padding
    val huge: Dp = 48.dp,          // 48dp — Hero sections, splash padding
    val massive: Dp = 64.dp        // 64dp — Full-screen illustration padding
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
