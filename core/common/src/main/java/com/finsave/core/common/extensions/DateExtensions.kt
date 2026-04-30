package com.finsave.core.common.extensions

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Date & Time extensions for FinSave.
 *
 * Key design decisions:
 * - All dates use java.time (minSdk 26 guarantees availability)
 * - Indian Financial Year (April–March) is the default
 * - Relative time formatting for "2 hours ago" display
 * - All formatters are thread-safe (java.time is immutable)
 */

// ── IST Zone (India Standard Time) ─────────────────────────────────────────
val IST: ZoneId = ZoneId.of("Asia/Kolkata")

// ── Common Formatters ──────────────────────────────────────────────────────
private val DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val DATE_SHORT_FORMAT = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, h:mm a", Locale.ENGLISH)
private val MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DAY_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM", Locale.ENGLISH)

// ── Date Display Extensions ────────────────────────────────────────────────

/**
 * "28 Apr 2025"
 */
fun LocalDate.toDisplayString(): String = format(DATE_DISPLAY_FORMAT)

/**
 * "28 Apr"
 */
fun LocalDate.toShortString(): String = format(DATE_SHORT_FORMAT)

/**
 * "April 2025"
 */
fun LocalDate.toMonthYearString(): String = format(MONTH_YEAR_FORMAT)

/**
 * "Monday, 28 Apr"
 */
fun LocalDate.toDayDateString(): String = format(DAY_DATE_FORMAT)

/**
 * "28 Apr 2025, 3:45 PM"
 */
fun LocalDateTime.toDisplayString(): String = format(DATE_TIME_FORMAT)

/**
 * "3:45 PM"
 */
fun LocalDateTime.toTimeString(): String = format(TIME_FORMAT)

// ── Relative Time ──────────────────────────────────────────────────────────

/**
 * Converts epoch millis to a human-readable relative time string.
 *
 * - < 1 minute: "Just now"
 * - < 1 hour: "23 minutes ago"
 * - < 24 hours: "3 hours ago"
 * - Yesterday: "Yesterday"
 * - < 7 days: "3 days ago"
 * - Otherwise: "28 Apr 2025"
 */
fun Long.toRelativeTimeString(): String {
    val now = Instant.now()
    val then = Instant.ofEpochMilli(this)
    val minutes = ChronoUnit.MINUTES.between(then, now)
    val hours = ChronoUnit.HOURS.between(then, now)
    val days = ChronoUnit.DAYS.between(then, now)

    val todayDate = LocalDate.now(IST)
    val thenDate = then.atZone(IST).toLocalDate()

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 && thenDate == todayDate -> "$hours hr ago"
        days == 1L || thenDate == todayDate.minusDays(1) -> "Yesterday"
        days < 7 -> "$days days ago"
        else -> thenDate.toDisplayString()
    }
}

/**
 * Converts epoch millis to LocalDate in IST.
 */
fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(IST).toLocalDate()

/**
 * Converts epoch millis to LocalDateTime in IST.
 */
fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(IST).toLocalDateTime()

/**
 * Converts LocalDate to epoch millis at start of day in IST.
 */
fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(IST).toInstant().toEpochMilli()

// ── Indian Financial Year ──────────────────────────────────────────────────

/**
 * Returns the Indian Financial Year for a given date.
 * FY runs April 1 – March 31.
 *
 * Example: January 2025 → FY 2024-25, July 2025 → FY 2025-26
 *
 * @return Pair of (startYear, endYear), e.g., (2024, 2025) for FY 2024-25
 */
fun LocalDate.getIndianFinancialYear(): Pair<Int, Int> {
    return if (monthValue >= 4) {
        Pair(year, year + 1)
    } else {
        Pair(year - 1, year)
    }
}

/**
 * Returns display string for Indian FY: "FY 2024-25"
 */
fun LocalDate.getFinancialYearDisplay(): String {
    val (start, end) = getIndianFinancialYear()
    return "FY $start-${end % 100}"
}

/**
 * Returns the start date of the Indian Financial Year containing this date.
 */
fun LocalDate.getFinancialYearStart(): LocalDate {
    val (startYear, _) = getIndianFinancialYear()
    return LocalDate.of(startYear, 4, 1)
}

/**
 * Returns the end date of the Indian Financial Year containing this date.
 */
fun LocalDate.getFinancialYearEnd(): LocalDate {
    val (_, endYear) = getIndianFinancialYear()
    return LocalDate.of(endYear, 3, 31)
}

// ── Month Navigation ───────────────────────────────────────────────────────

/**
 * Returns the first day of the month for this date.
 */
fun LocalDate.startOfMonth(): LocalDate = withDayOfMonth(1)

/**
 * Returns the last day of the month for this date.
 */
fun LocalDate.endOfMonth(): LocalDate = withDayOfMonth(lengthOfMonth())

/**
 * Returns the first day of the week (Monday) for this date.
 */
fun LocalDate.startOfWeek(): LocalDate {
    val dayOfWeek = dayOfWeek.value // Monday = 1, Sunday = 7
    return minusDays((dayOfWeek - 1).toLong())
}

/**
 * Returns the last day of the week (Sunday) for this date.
 */
fun LocalDate.endOfWeek(): LocalDate {
    val dayOfWeek = dayOfWeek.value
    return plusDays((7 - dayOfWeek).toLong())
}
