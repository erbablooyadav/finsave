package com.finsave.domain.usecase.category

import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.usecase.sms.extractors.AutoCategoriser
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SuggestCategoryForMerchantUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val autoCategoriser: AutoCategoriser
) {
    suspend operator fun invoke(merchantName: String): Long? {
        if (merchantName.isBlank()) return null
        
        val mappings = categoryRepository.getAutoCategoryMappings()
        val suggestedCategoryName = autoCategoriser.categorise(merchantName, mappings)
        
        val categories = categoryRepository.getAllCategories().first()
        val match = categories.find { it.name.equals(suggestedCategoryName, ignoreCase = true) }
        
        // Return the matched category ID, or null if not found (e.g., Miscellaneous might not exist)
        return match?.id
    }
}
