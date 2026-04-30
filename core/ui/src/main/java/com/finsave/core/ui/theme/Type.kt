package com.finsave.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

/**
 * FinSave Typography System
 *
 * Uses Inter from Google Fonts — clean, modern, excellent number rendering.
 * Fallback to system sans-serif if Google Fonts unavailable (offline-first).
 *
 * Material 3 Type Scale with FinSave-specific customizations:
 * - Financial amounts use tabular figures for aligned columns
 * - Large display numbers for dashboard headers
 * - Compact body text for transaction lists
 */

// ── Google Fonts Provider ──────────────────────────────────────────────────
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.finsave.core.ui.R.array.com_google_android_gms_fonts_certs
)

// ── Font Families ──────────────────────────────────────────────────────────
private val InterFont = GoogleFont("Inter")

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold),
)

// Outfit for display/headline text — gives FinSave a distinctive personality
private val OutfitFont = GoogleFont("Outfit")

val OutfitFontFamily = FontFamily(
    Font(googleFont = OutfitFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
    Font(googleFont = OutfitFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = OutfitFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = OutfitFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = OutfitFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

// ── Material 3 Typography ──────────────────────────────────────────────────
val FinSaveTypography = Typography(

    // ── Display ────────────────────────────────────────────────────
    // Used for: Dashboard total amount, FinSave Score number
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // ── Headline ───────────────────────────────────────────────────
    // Used for: Section headers, card titles
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // ── Title ──────────────────────────────────────────────────────
    // Used for: Top bar titles, dialog titles, budget category names
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // ── Body ───────────────────────────────────────────────────────
    // Used for: Transaction descriptions, settings text, general content
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // ── Label ──────────────────────────────────────────────────────
    // Used for: Button text, chip labels, category names
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
