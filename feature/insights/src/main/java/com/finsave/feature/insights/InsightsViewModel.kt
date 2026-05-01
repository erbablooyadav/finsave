package com.finsave.feature.insights

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.domain.model.Category
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.repository.TransactionRepository
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class InsightSegment(
    val category: Category,
    val amountPaise: Long,
    val color: Color
)

data class MonthlyTrendPoint(val monthLabel: String, val debitPaise: Long)

data class TopMerchantRow(val name: String, val totalPaise: Long, val txCount: Int)

data class DayOfWeekRow(val dayLabel: String, val totalPaise: Long)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getSpendingSummaryUseCase: GetSpendingSummaryUseCase,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: com.finsave.core.common.prefs.PreferencesManager
) : ViewModel() {

    private val istZone = ZoneId.of("Asia/Kolkata")
    private val currentMonth = YearMonth.now(istZone)
    private val startOfMonth = currentMonth.atDay(1)
    private val endOfMonth = currentMonth.atEndOfMonth()
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM")

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

    val monthlyTrend: StateFlow<List<MonthlyTrendPoint>> = run {
        val months = (5 downTo 0).map { currentMonth.minusMonths(it.toLong()) }
        val startDate = months.first().atDay(1)
        val endDate = months.last().atEndOfMonth()

        transactionRepository.getTransactionsByDateRange(startDate, endDate)
            .map { transactions ->
                months.map { month ->
                    val total = transactions
                        .asSequence()
                        .filter { it.type == TransactionType.DEBIT }
                        .filter { YearMonth.from(it.date) == month }
                        .sumOf { it.amountPaise }

                    MonthlyTrendPoint(
                        monthLabel = month.format(monthLabelFormatter),
                        debitPaise = total
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    val topMerchants: StateFlow<List<TopMerchantRow>> =
        transactionRepository.getTransactionsByDateRange(startOfMonth, endOfMonth)
            .map { transactions ->
                transactions
                    .asSequence()
                    .filter { it.type == TransactionType.DEBIT }
                    .map { it.merchantName.trim() to it.amountPaise }
                    .filter { (merchant, _) -> merchant.isNotBlank() }
                    .groupBy { (merchant, _) -> merchant.lowercase() }
                    .map { (_, entries) ->
                        TopMerchantRow(
                            name = entries.first().first,
                            totalPaise = entries.sumOf { it.second },
                            txCount = entries.size
                        )
                    }
                    .sortedByDescending { it.totalPaise }
                    .take(5)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayOfWeekSpend: StateFlow<List<DayOfWeekRow>> = run {
        val today = LocalDate.now(istZone)
        val startDate = today.minusDays(29)
        val labels = linkedMapOf(
            DayOfWeek.MONDAY to "Mon",
            DayOfWeek.TUESDAY to "Tue",
            DayOfWeek.WEDNESDAY to "Wed",
            DayOfWeek.THURSDAY to "Thu",
            DayOfWeek.FRIDAY to "Fri",
            DayOfWeek.SATURDAY to "Sat",
            DayOfWeek.SUNDAY to "Sun"
        )

        transactionRepository.getTransactionsByDateRange(startDate, today)
            .map { transactions ->
                val totals = transactions
                    .asSequence()
                    .filter { it.type == TransactionType.DEBIT }
                    .groupBy { it.date.dayOfWeek }
                    .mapValues { (_, dayTransactions) -> dayTransactions.sumOf { it.amountPaise } }

                labels.map { (day, label) ->
                    DayOfWeekRow(
                        dayLabel = label,
                        totalPaise = totals[day] ?: 0L
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()
}
