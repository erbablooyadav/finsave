package com.finsave.domain.usecase.sms.extractors

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoCategoriser @Inject constructor() {

    /**
     * Attempts to find a category for a merchant based on the provided mappings.
     * @param merchant The extracted merchant name.
     * @param mappings A map of lowercase keywords to category names.
     */
    fun categorise(merchant: String, mappings: Map<String, String>): String {
        val lowerMerchant = merchant.lowercase()
        
        // Exact match first
        mappings[lowerMerchant]?.let { return it }
        
        // Substring match
        for ((keyword, category) in mappings) {
            if (lowerMerchant.contains(keyword.lowercase())) {
                return category
            }
        }
        
        return "Miscellaneous"
    }
}
