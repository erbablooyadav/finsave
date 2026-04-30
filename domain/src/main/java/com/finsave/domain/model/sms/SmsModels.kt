package com.finsave.domain.model.sms

/**
 * Domain models for parsing SMS based on the bank_patterns.json configuration.
 */

data class BankPatternConfig(
    val version: Int,
    val lastUpdated: String,
    val banks: List<BankConfig>,
    val autoCategoryMappings: Map<String, String>
)

data class BankConfig(
    val bankName: String,
    val senderIds: List<String>,
    val patterns: List<SmsPattern>
)

data class SmsPattern(
    val type: TransactionType,
    val regex: String,
    val amountGroup: Int = 1,
    val merchantRegex: String? = null,
    val upiRegex: String? = null
)

enum class TransactionType {
    DEBIT, CREDIT, TRANSFER, FAILED
}
