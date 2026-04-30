package com.finsave.core.common.formatter

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Indian Number Formatter — Handles the Indian numbering system (Lakhs & Crores).
 *
 * Indian system: 1,00,00,000 (1 Crore)
 * International: 10,000,000 (10 Million)
 *
 * All amounts are stored internally as Long in paise (1 INR = 100 paise).
 * This eliminates floating-point precision issues entirely.
 */
object IndianNumberFormatter {

    private const val PAISE_DIVISOR = 100.0

    // ── Indian Format Thresholds ────────────────────────────────────
    private const val THOUSAND = 1_000L
    private const val LAKH = 1_00_000L
    private const val CRORE = 1_00_00_000L

    /**
     * Formats paise amount to Indian rupee display string.
     *
     * @param paiseAmount Amount in paise (e.g., 123456 = ₹1,234.56)
     * @param useIndianSystem If true, uses Indian comma grouping (1,23,456.78)
     * @param showPaise If true, shows decimal places
     * @param showSymbol If true, prefixes with ₹
     * @return Formatted string like "₹1,23,456.78" or "₹1,23,457"
     */
    fun format(
        paiseAmount: Long,
        useIndianSystem: Boolean = true,
        showPaise: Boolean = false,
        showSymbol: Boolean = true
    ): String {
        val isNegative = paiseAmount < 0
        val absolutePaise = kotlin.math.abs(paiseAmount)
        val rupees = absolutePaise / 100
        val paise = absolutePaise % 100

        val formattedRupees = if (useIndianSystem) {
            formatIndian(rupees)
        } else {
            formatInternational(rupees)
        }

        val builder = StringBuilder()
        if (isNegative) builder.append("-")
        if (showSymbol) builder.append("₹")
        builder.append(formattedRupees)

        if (showPaise) {
            builder.append(".")
            builder.append(String.format(Locale.ROOT, "%02d", paise))
        }

        return builder.toString()
    }

    /**
     * Formats rupees (not paise) using Indian comma grouping.
     * Pattern: Last 3 digits, then groups of 2.
     * Example: 1234567 → "12,34,567"
     */
    private fun formatIndian(rupees: Long): String {
        if (rupees < THOUSAND) return rupees.toString()

        val lastThreeDigits = rupees % 1000
        var remaining = rupees / 1000
        val groups = mutableListOf<String>()

        groups.add(String.format(Locale.ROOT, "%03d", lastThreeDigits))

        while (remaining > 0) {
            val group = remaining % 100
            remaining /= 100
            if (remaining > 0) {
                groups.add(String.format(Locale.ROOT, "%02d", group))
            } else {
                groups.add(group.toString())
            }
        }

        return groups.reversed().joinToString(",")
    }

    /**
     * Formats rupees using international comma grouping.
     * Pattern: Groups of 3 from right.
     * Example: 1234567 → "1,234,567"
     */
    private fun formatInternational(rupees: Long): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,###", symbols)
        return formatter.format(rupees)
    }

    /**
     * Formats amount for compact display (e.g., "₹1.2L", "₹3.5Cr").
     * Used in charts and summary cards where space is limited.
     */
    fun formatCompact(paiseAmount: Long, showSymbol: Boolean = true): String {
        val rupees = kotlin.math.abs(paiseAmount) / 100
        val prefix = if (paiseAmount < 0) "-" else ""
        val symbol = if (showSymbol) "₹" else ""

        return when {
            rupees >= CRORE -> {
                val crores = rupees.toDouble() / CRORE
                "${prefix}${symbol}${String.format(Locale.ROOT, "%.1f", crores)}Cr"
            }
            rupees >= LAKH -> {
                val lakhs = rupees.toDouble() / LAKH
                "${prefix}${symbol}${String.format(Locale.ROOT, "%.1f", lakhs)}L"
            }
            rupees >= THOUSAND -> {
                val thousands = rupees.toDouble() / THOUSAND
                "${prefix}${symbol}${String.format(Locale.ROOT, "%.1f", thousands)}K"
            }
            else -> "${prefix}${symbol}$rupees"
        }
    }

    /**
     * Parses an amount string (from SMS or user input) into paise.
     * Handles: "1,23,456.78", "₹1234", "Rs. 500", "INR 1000.50"
     *
     * @return Amount in paise, or null if parsing fails
     */
    fun parseToPaise(amountString: String): Long? {
        val cleaned = amountString
            .replace("₹", "")
            .replace("Rs.", "")
            .replace("Rs", "")
            .replace("INR", "")
            .replace(",", "")
            .replace(" ", "")
            .trim()

        return try {
            // Use BigDecimal to avoid floating-point precision issues
            val bigDecimal = java.math.BigDecimal(cleaned)
            val paiseDecimal = bigDecimal.multiply(java.math.BigDecimal("100"))
            paiseDecimal.setScale(0, java.math.RoundingMode.HALF_UP).toLong()
        } catch (e: NumberFormatException) {
            null
        } catch (e: ArithmeticException) {
            null
        }
    }

    /**
     * Formats amount for TalkBack accessibility reading.
     * "₹1,23,456" reads as "Rupees 1 lakh 23 thousand 456"
     */
    fun formatForAccessibility(paiseAmount: Long): String {
        val rupees = kotlin.math.abs(paiseAmount) / 100
        val paise = kotlin.math.abs(paiseAmount) % 100
        val prefix = if (paiseAmount < 0) "minus " else ""

        val parts = mutableListOf<String>()

        val crores = rupees / CRORE
        val afterCrore = rupees % CRORE
        val lakhs = afterCrore / LAKH
        val afterLakh = afterCrore % LAKH
        val thousands = afterLakh / THOUSAND
        val remainder = afterLakh % THOUSAND

        if (crores > 0) parts.add("$crores crore")
        if (lakhs > 0) parts.add("$lakhs lakh")
        if (thousands > 0) parts.add("$thousands thousand")
        if (remainder > 0) parts.add("$remainder")

        val rupeeText = if (parts.isEmpty()) "zero" else parts.joinToString(" ")

        return if (paise > 0) {
            "${prefix}Rupees $rupeeText and $paise paise"
        } else {
            "${prefix}Rupees $rupeeText"
        }
    }
}
