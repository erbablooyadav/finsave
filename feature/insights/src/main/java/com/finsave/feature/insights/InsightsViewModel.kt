package com.finsave.feature.insights

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Category
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.YearMonth
import javax.inject.Inject

data class InsightSegment(
    val category: Category,
    val amountPaise: Long,
    val color: Color
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getSpendingSummaryUseCase: GetSpendingSummaryUseCase,
    private val categoryRepository: CategoryRepository,
    private val preferencesManager: com.finsave.core.common.prefs.PreferencesManager
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val startOfMonth = currentMonth.atDay(1)
    private val endOfMonth = currentMonth.atEndOfMonth()

    val totalSpendPaise = getSpendingSummaryUseCase.getTotalDebit(startOfMonth, endOfMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val segments = combine(
        getSpendingSummaryUseCase.getCategorySpendSummary(startOfMonth, endOfMonth),
        categoryRepository.getAllCategories()
    ) { summaries, categories ->
        val categoryMap = categories.associateBy { it.id }
        summaries.map { summary ->
            val category = categoryMap[summary.categoryId] ?: Category(name = "Unknown", emoji = "❓", colorHex = "#9E9E9E")
            val color = try {
                Color(android.graphics.Color.parseColor(category.colorHex))
            } catch (e: Exception) {
                Color.Gray
            }
            InsightSegment(
                category = category,
                amountPaise = summary.totalSpentPaise,
                color = color
            )
        }.sortedByDescending { it.amountPaise }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()
}
