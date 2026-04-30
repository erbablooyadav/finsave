package com.finsave.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * FinSave Color System
 *
 * Design philosophy from PRD Section 6.1:
 * - Primary: Indigo (#4F46E5) — trust, stability, fintech authority
 * - Semantic: Red for debit, Green for credit — universal finance convention
 * - Dark mode: OLED-optimised with slightly desaturated red/green
 * - No generic colors — every color has a purpose
 */

// ── Primary Palette ────────────────────────────────────────────────────────
val Indigo50 = Color(0xFFEEF2FF)
val Indigo100 = Color(0xFFE0E7FF)
val Indigo200 = Color(0xFFC7D2FE)
val Indigo300 = Color(0xFFA5B4FC)
val Indigo400 = Color(0xFF818CF8)
val Indigo500 = Color(0xFF6366F1)
val Indigo600 = Color(0xFF4F46E5)  // ← PRIMARY — The FinSave Indigo
val Indigo700 = Color(0xFF4338CA)
val Indigo800 = Color(0xFF3730A3)
val Indigo900 = Color(0xFF312E81)

// ── Secondary Palette (Teal — Fresh, modern complement to Indigo) ──────────
val Teal50 = Color(0xFFF0FDFA)
val Teal100 = Color(0xFFCCFBF1)
val Teal200 = Color(0xFF99F6E4)
val Teal300 = Color(0xFF5EEAD4)
val Teal400 = Color(0xFF2DD4BF)
val Teal500 = Color(0xFF14B8A6)
val Teal600 = Color(0xFF0D9488)
val Teal700 = Color(0xFF0F766E)

// ── Tertiary Palette (Amber — Warm accent for alerts and highlights) ───────
val Amber50 = Color(0xFFFFFBEB)
val Amber100 = Color(0xFFFEF3C7)
val Amber200 = Color(0xFFFDE68A)
val Amber300 = Color(0xFFFCD34D)
val Amber400 = Color(0xFFFBBF24)
val Amber500 = Color(0xFFF59E0B)
val Amber600 = Color(0xFFD97706)
val Amber700 = Color(0xFFB45309)

// ── Semantic Colors — Light Mode ───────────────────────────────────────────
val DebitRed = Color(0xFFEF4444)        // Transaction debit
val DebitRedLight = Color(0xFFFEE2E2)   // Debit background tint
val CreditGreen = Color(0xFF22C55E)     // Transaction credit
val CreditGreenLight = Color(0xFFDCFCE7) // Credit background tint
val WarningAmber = Color(0xFFF59E0B)    // Budget 70-90%
val WarningAmberLight = Color(0xFFFEF3C7)
val DangerRed = Color(0xFFDC2626)       // Budget 90%+
val SuccessGreen = Color(0xFF16A34A)    // Group settled, streak active

// ── Semantic Colors — Dark Mode (OLED optimised, slightly desaturated) ─────
val DebitRedDark = Color(0xFFF87171)     // Desaturated for OLED
val CreditGreenDark = Color(0xFF4ADE80)  // Desaturated for OLED
val WarningAmberDark = Color(0xFFFBBF24)
val DangerRedDark = Color(0xFFEF4444)
val SuccessGreenDark = Color(0xFF22C55E)

// ── Surface Colors — Light Mode ────────────────────────────────────────────
val SurfaceLight = Color(0xFFF9FAFB)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFF3F4F6)
val OnSurfaceLight = Color(0xFF111827)
val OnSurfaceVariantLight = Color(0xFF6B7280)

// ── Surface Colors — Dark Mode (PRD Section 6.2) ───────────────────────────
val SurfaceDark = Color(0xFF111928)             // Dark background from PRD
val SurfaceContainerDark = Color(0xFF1F2937)    // Surface from PRD
val SurfaceContainerHighDark = Color(0xFF374151)
val OnSurfaceDark = Color(0xFFF9FAFB)
val OnSurfaceVariantDark = Color(0xFF9CA3AF)

// ── Category Colors (18 default categories) ────────────────────────────────
val CategoryFoodDining = Color(0xFFFF6B6B)
val CategoryTransport = Color(0xFF4ECDC4)
val CategoryShopping = Color(0xFFFFE66D)
val CategoryEntertainment = Color(0xFFA78BFA)
val CategoryBillsUtilities = Color(0xFF60A5FA)
val CategoryHealth = Color(0xFFF472B6)
val CategoryEducation = Color(0xFF34D399)
val CategoryRent = Color(0xFFFB923C)
val CategoryGroceries = Color(0xFF86EFAC)
val CategoryInvestments = Color(0xFF818CF8)
val CategorySalary = Color(0xFF22D3EE)
val CategoryFreelance = Color(0xFFFCA5A5)
val CategoryGifts = Color(0xFFF9A8D4)
val CategoryTravel = Color(0xFF67E8F9)
val CategoryPersonalCare = Color(0xFFC4B5FD)
val CategoryInsurance = Color(0xFF93C5FD)
val CategoryEmi = Color(0xFFFDA4AF)
val CategoryOther = Color(0xFF94A3B8)

// ── Streak & Gamification Colors ───────────────────────────────────────────
val StreakFire = Color(0xFFFF6B35)
val StreakGold = Color(0xFFFFD700)
val ScoreGradientStart = Color(0xFF4F46E5)
val ScoreGradientEnd = Color(0xFF7C3AED)
