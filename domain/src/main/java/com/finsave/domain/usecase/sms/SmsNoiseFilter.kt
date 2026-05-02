package com.finsave.domain.usecase.sms

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pre-pass noise filter for bank SMS messages.
 *
 * Applied BEFORE any amount extraction so that non-actionable messages are
 * discarded cheaply. Returning `true` means "this is noise — skip it."
 *
 * Categories of noise:
 *  1. Credit-card payment confirmation   — "received towards your credit card"
 *     The savings-account DEBIT has its own separate SMS already captured.
 *  2. Available / credit limit info      — "available limit", "credit limit"
 *  3. OTP / PIN messages                 — "OTP", "one time password", "do not share"
 *  4. Balance-enquiry replies            — "your balance is", "available bal"
 *  5. Last-transaction info / mini-stmt  — "mini statement", "last txn on"
 *  6. Transaction-failed / declined      — "failed", "declined", "unsuccessful"
 *  7. Promotional / marketing            — "offer", "cashback offer", "click here"
 *  8. Reward/points credit               — "reward points", "cashback credited"
 */
@Singleton
class SmsNoiseFilter @Inject constructor() {

    /**
     * Returns `true` if the [body] is a noise SMS that should NOT be parsed
     * as a financial transaction.
     */
    fun isNoise(body: String): Boolean {
        val lower = body.lowercase()
        return NOISE_PATTERNS.any { lower.contains(it) }
    }

    companion object {
        /**
         * Ordered from most-specific to most-general to keep false-positive risk low.
         * All patterns are matched against the lowercase SMS body.
         */
        private val NOISE_PATTERNS = listOf(
            // ── Credit card payment confirmations ───────────────────────────
            "towards your credit card",
            "payment towards credit card",
            "credit card payment received",
            "credit card payment of",
            "payment received for credit card",
            "towards cc",
            // ── Available / credit limit info (standalone) ──────────────────
            "available credit limit",
            "credit limit is",
            "available limit is",
            "total credit limit",
            "credit limit:",
            // ── Balance enquiry / account balance info ──────────────────────
            "your account balance is",
            "your balance is",
            "available balance:",
            "available bal:",
            "available bal is",
            "account bal:",
            "acct bal:",
            "bal is rs",
            "balance update",
            // ── OTP / authentication messages ───────────────────────────────
            " otp ",
            ":otp",
            "otp is",
            "otp for",
            "one time password",
            "one-time password",
            "do not share",
            "do not disclose",
            "never share",
            "secret pin",
            " pin for ",
            "your pin is",
            // ── Mini statement / last-transaction info ──────────────────────
            "mini statement",
            "last txn on",
            "last transaction:",
            "statement generated",
            // ── Transaction failed / declined ───────────────────────────────
            "transaction failed",
            "transaction declined",
            "transaction unsuccessful",
            "payment failed",
            "payment declined",
            "payment unsuccessful",
            "could not be processed",
            "insufficient funds",
            "insufficient balance",
            // ── Reward points / cashback (not actual money movement) ────────
            "reward point",
            "reward pts",
            "cashback will be",
            "cashback of rs",
            // ── Promotional / marketing ─────────────────────────────────────
            "click here to",
            "visit our website",
            "download our app",
            "limited offer",
            "exclusive offer",
            "pre-approved",
            "pre approved",
            "get up to",
            "earn up to",
            "unsubscribe",
            // ── Inward NEFT / RTGS credit limit update (not the transfer itself) ─
            "your updated limit",
            "revised limit",
        )
    }
}
