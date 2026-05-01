package com.finsave.domain.usecase.sms

import com.finsave.domain.model.sms.BankPatternConfig
import com.finsave.domain.model.sms.TransactionType
import com.finsave.domain.usecase.sms.extractors.AmountExtractor
import com.finsave.domain.usecase.sms.extractors.AutoCategoriser
import com.finsave.domain.usecase.sms.extractors.MerchantExtractor
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTransaction(
    val smsId: String,
    val bankName: String,
    val amountPaise: Long,
    val type: TransactionType,
    val merchant: String,
    val category: String,
    val timestamp: Long,
    val duplicateHash: String
)

@Singleton
class SmsParserEngine @Inject constructor(
    private val amountExtractor: AmountExtractor,
    private val merchantExtractor: MerchantExtractor,
    private val autoCategoriser: AutoCategoriser
) {
    /**
     * Parses a raw SMS body into a ParsedTransaction.
     * Returns null if the SMS does not match any known bank patterns.
     */
    fun parse(
        smsId: String,
        sender: String,
        body: String,
        timestamp: Long,
        config: BankPatternConfig
    ): ParsedTransaction? {
        
        // 1. Try to Identify Bank by Sender ID
        val bank = config.banks.find { bankConfig ->
            bankConfig.senderIds.any { id -> sender.contains(id, ignoreCase = true) }
        }

        if (bank != null) {
            // 2. Try to match SMS body against strict bank patterns
            for (pattern in bank.patterns) {
                val matcher = Regex(pattern.regex, RegexOption.IGNORE_CASE)
                val matchResult = matcher.find(body)
                
                if (matchResult != null && matchResult.groups.size > pattern.amountGroup) {
                    val amountStr = matchResult.groups[pattern.amountGroup]?.value ?: continue
                    val amountPaise = amountExtractor.extractAmount(amountStr) ?: continue
                    
                    val merchant = merchantExtractor.extractMerchant(
                        smsBody = body,
                        merchantRegex = pattern.merchantRegex,
                        upiRegex = pattern.upiRegex
                    )
                    
                    val category = autoCategoriser.categorise(merchant, config.autoCategoryMappings)
                    val rawToHash = "$amountPaise|${pattern.type}|$merchant|$timestamp"
                    val duplicateHash = hashString(rawToHash)
                    
                    return ParsedTransaction(
                        smsId = smsId,
                        bankName = bank.bankName,
                        amountPaise = amountPaise,
                        type = pattern.type,
                        merchant = merchant,
                        category = category,
                        timestamp = timestamp,
                        duplicateHash = duplicateHash
                    )
                }
            }
        }

        if (bank == null) {
            return null
        }

        // --- UNIVERSAL FALLBACK HEURISTIC ---
        // If strict config fails for a known sender, use universal heuristics to catch transactions.
        
        val amountRegex = Regex("(?i)(?:Rs\\.?|INR|₹|Rs)\\s*:?\\s*([\\d,]+\\.?\\d*)")
        val debitRegex = Regex("(?i)(debited|withdrawn|spent|paid|sent|txn)")
        val creditRegex = Regex("(?i)(credited|received|refunded|deposited)")
        val merchantRegex = Regex("(?i)(?:at|to|info|by|vpa|upi|ref)\\s+([A-Za-z0-9@.\\-\\s]+?)(?=\\s+(?:by|on|ref|avl|not|call|-|/))")
        val simpleMerchantRegex = Regex("(?i)(?:at|to)\\s+([A-Za-z0-9@.\\-]+)")

        val amountMatch = amountRegex.find(body)
        if (amountMatch != null) {
            val amountStr = amountMatch.groupValues.getOrNull(1) ?: return null
            val amountPaise = amountExtractor.extractAmount(amountStr) ?: return null

            val isDebit = debitRegex.containsMatchIn(body)
            val isCredit = creditRegex.containsMatchIn(body)
            
            // Must be strictly one or the other (or defaults to Debit if it's "Txn")
            val type = when {
                isCredit && !isDebit -> TransactionType.CREDIT
                isDebit -> TransactionType.DEBIT
                else -> return null // Ambiguous or not a transaction
            }

            var merchant = merchantRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
            if (merchant.isNullOrEmpty()) {
                merchant = simpleMerchantRegex.find(body)?.groupValues?.getOrNull(1)?.trim()
            }
            if (merchant.isNullOrEmpty()) {
                merchant = "Unknown"
            }

            // Clean up merchant name slightly
            merchant = merchant.trimEnd(',', '.', '-')

            // Determine an approximate bank name from sender ID if unknown
            val bankName = bank?.bankName ?: sender.replace(Regex("^[A-Za-z]{2}-"), "").uppercase()

            val category = autoCategoriser.categorise(merchant, config.autoCategoryMappings)
            val rawToHash = "$amountPaise|$type|$merchant|$timestamp"
            val duplicateHash = hashString(rawToHash)

            return ParsedTransaction(
                smsId = smsId,
                bankName = bankName,
                amountPaise = amountPaise,
                type = type,
                merchant = merchant,
                category = category,
                timestamp = timestamp,
                duplicateHash = duplicateHash
            )
        }

        return null
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
