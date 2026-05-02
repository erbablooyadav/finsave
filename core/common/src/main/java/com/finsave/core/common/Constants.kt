package com.finsave.core.common

/**
 * FinSave Application Constants
 *
 * Centralised constants used across all modules.
 * No hardcoded strings scattered in the codebase.
 */
object Constants {

    // ── Database ───────────────────────────────────────────────────
    const val DATABASE_NAME = "finsave_database"
    const val DATABASE_VERSION = 1

    // ── Preferences ────────────────────────────────────────────────
    const val PREFS_NAME = "finsave_prefs"
    const val PREFS_ONBOARDING_COMPLETE = "onboarding_complete"
    const val PREFS_USE_INDIAN_NUMBER_SYSTEM = "use_indian_number_system"
    const val PREFS_FINANCIAL_YEAR_APRIL = "financial_year_april_start"
    const val PREFS_DARK_MODE = "dark_mode"
    const val PREFS_APP_LOCK_ENABLED = "app_lock_enabled"
    const val PREFS_AUTO_LOCK_TIMEOUT = "auto_lock_timeout_seconds"
    const val PREFS_LAST_PAUSED_AT = "last_paused_at_millis"
    const val PREFS_BUDGET_STREAK_COUNT = "budget_streak_count"
    const val PREFS_LAST_STREAK_DATE = "last_streak_date"
    const val PREFS_FINSAVE_SCORE = "finsave_score"
    const val PREFS_LAST_SMS_IMPORT_COMPLETION_ID = "last_sms_import_completion_id"
    const val PREFS_LAST_SMS_SYNC_TIMESTAMP = "last_sms_sync_timestamp_millis"
    const val PREFS_DB_ENCRYPTED_V1 = "db_encrypted_v1"
    const val PREFS_DB_PASSPHRASE_IV = "db_passphrase_iv_b64"

    // ── SMS Parsing ────────────────────────────────────────────────
    const val BANK_PATTERNS_ASSET = "bank_patterns.json"
    const val SMS_READ_BATCH_SIZE = 500
    const val SMS_MAX_AGE_DAYS = 90L // Only parse last 90 days of SMS

    // ── Notifications ──────────────────────────────────────────────
    const val NOTIFICATION_CHANNEL_BUDGET = "finsave_budget"
    const val NOTIFICATION_CHANNEL_SPLITTER = "finsave_splitter"
    const val NOTIFICATION_CHANNEL_INSIGHTS = "finsave_insights"
    const val NOTIFICATION_CHANNEL_DAILY = "finsave_daily_digest"
    const val DAILY_DIGEST_ID = 1001

    // ── Budget Thresholds ──────────────────────────────────────────
    const val BUDGET_WARNING_THRESHOLD = 0.80   // 80% — amber warning
    const val BUDGET_DANGER_THRESHOLD = 0.90    // 90% — red danger
    const val BUDGET_EXCEEDED_THRESHOLD = 1.00  // 100% — over budget

    // ── Splitter ───────────────────────────────────────────────────
    const val SPLITTER_REMINDER_THRESHOLD_PAISE = 50000L // ₹500 in paise
    const val SPLITTER_REMINDER_DAYS = 7

    // ── Categories ─────────────────────────────────────────────────
    const val MAX_CUSTOM_CATEGORIES = 50
    const val DEFAULT_CATEGORY_COUNT = 18

    // ── Export ──────────────────────────────────────────────────────
    const val CSV_BOM = "\uFEFF" // UTF-8 BOM for Excel compatibility
    const val CSV_MIME_TYPE = "text/csv"
    const val PDF_MIME_TYPE = "application/pdf"

    // ── Navigation Routes ──────────────────────────────────────────
    object Routes {
        const val ONBOARDING = "onboarding"
        const val DASHBOARD = "dashboard"
        const val TRANSACTIONS = "transactions"
        const val ADD_TRANSACTION = "add_transaction"
        const val ACCOUNTS = "accounts"
        const val CATEGORIES = "categories"
        const val BUDGET = "budget"
        const val INSIGHTS = "insights"
        const val SPLITTER = "splitter"
        const val SPLITTER_GROUP = "splitter_group/{groupId}"
        const val SETTINGS = "settings"
    }

    // ── WorkManager Tags ───────────────────────────────────────────
    const val WORK_SMS_SYNC = "finsave_sms_sync"
    const val WORK_SMS_MANUAL_IMPORT = "finsave_sms_manual_import"
    const val WORK_BUDGET_CHECK = "finsave_budget_check"
    const val WORK_DAILY_DIGEST = "finsave_daily_digest"
    const val WORK_SPLITTER_REMINDER = "finsave_splitter"

    // ── WorkManager Progress Keys ─────────────────────────────────
    const val PROGRESS_SMS_TOTAL = "progress_sms_total"
    const val PROGRESS_SMS_PARSED = "progress_sms_parsed"
    const val PROGRESS_SMS_IMPORTED = "progress_sms_imported"

    // ── Billing ────────────────────────────────────────────────────
    const val BILLING_PRODUCT_FINSAVE_PLUS = "finsave_plus"
    const val BILLING_PRODUCT_TIP_SMALL = "finsave_tip_small"
    const val BILLING_PRODUCT_TIP_MEDIUM = "finsave_tip_medium"
    const val BILLING_PRODUCT_TIP_LARGE = "finsave_tip_large"
}
