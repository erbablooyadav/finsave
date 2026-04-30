package com.finsave.domain.usecase.sms.extractors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MerchantExtractor @Inject constructor() {

    /**
     * Attempts to cleanly extract a merchant name.
     * Uses merchantRegex if matched. If missing, tries to infer from VPA (upiRegex).
     * Falls back to "Unknown Merchant" if both fail.
     */
    fun extractMerchant(
        smsBody: String,
        merchantRegex: String?,
        upiRegex: String?
    ): String {
        var merchantName: String? = null

        // Try explicit merchant regex (e.g. "to Swiggy on")
        if (!merchantRegex.isNullOrEmpty()) {
            val matcher = Regex(merchantRegex, RegexOption.IGNORE_CASE)
            val matchResult = matcher.find(smsBody)
            if (matchResult != null && matchResult.groups.size > 1) {
                merchantName = matchResult.groups[1]?.value?.trim()
                // Clean up common trailing artifacts
                merchantName = merchantName?.removeSuffix(".")?.removeSuffix(" on")?.trim()
            }
        }

        // If explicit merchant name is missing or invalid, try to infer from VPA (e.g., shopname@okhdfcbank)
        if (merchantName.isNullOrEmpty() && !upiRegex.isNullOrEmpty()) {
            val matcher = Regex(upiRegex, RegexOption.IGNORE_CASE)
            val matchResult = matcher.find(smsBody)
            if (matchResult != null && matchResult.groups.size > 1) {
                val vpa = matchResult.groups[1]?.value?.trim() ?: ""
                if (vpa.contains("@")) {
                    val handle = vpa.substringBefore("@")
                    
                    // Heuristics for common raw handles
                    val cleanHandle = handle.replace(".", "").lowercase()
                    
                    merchantName = when {
                        // Exactly 10 digits (likely a mobile number)
                        handle.matches(Regex("^[0-9]{10}$")) -> "UPI User ($handle)"
                        
                        // Mostly digits or generic alphanumeric without clear words
                        handle.all { it.isDigit() } -> "UPI User ($handle)"
                        
                        // Common payment gateways in VPA
                        cleanHandle.contains("paytm") -> "PayTM Merchant"
                        cleanHandle.contains("bharatpe") -> "BharatPe Merchant"
                        cleanHandle.contains("phonepe") -> "PhonePe Merchant"
                        cleanHandle.contains("amazon") -> "Amazon Pay Merchant"
                        cleanHandle.contains("cred") -> "CRED Merchant"
                        
                        else -> {
                            // Convert "zomato123" -> "Zomato123"
                            handle.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        }
                    }
                } else {
                    merchantName = vpa
                }
            }
        }

        return merchantName?.takeIf { it.isNotBlank() } ?: "Unknown Merchant"
    }
}
