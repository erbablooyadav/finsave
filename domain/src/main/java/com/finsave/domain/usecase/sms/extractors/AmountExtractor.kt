package com.finsave.domain.usecase.sms.extractors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmountExtractor @Inject constructor() {
    
    /**
     * Extracts amount in paise (Long) from the matched string.
     * Removes commas, handles decimals.
     * E.g., "1,234.50" -> 123450L
     * "500" -> 50000L
     */
    fun extractAmount(amountStr: String): Long? {
        val cleanStr = amountStr.replace(",", "").trim()
        val parts = cleanStr.split(".")
        
        return try {
            if (parts.size == 1) {
                // No decimal, just rupees
                parts[0].toLong() * 100L
            } else {
                // Has decimal
                val rupees = parts[0].toLong() * 100L
                // Take up to 2 decimal places and pad if necessary
                val decimalStr = parts[1].take(2).padEnd(2, '0')
                val paise = decimalStr.toLong()
                rupees + paise
            }
        } catch (e: Exception) {
            null
        }
    }
}
