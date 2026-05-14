package com.finsave.feature.insights

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finsave.core.common.share.PersonalityType
import com.finsave.core.common.share.SpendDnaData
import com.finsave.core.common.share.SpendDnaGenerator
import com.finsave.domain.model.Category
import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.BudgetRepository
import com.finsave.domain.repository.CategoryRepository
import com.finsave.domain.repository.TransactionRepository
import com.finsave.domain.usecase.transaction.GetSpendingSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    private val budgetRepository: BudgetRepository,
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
                Color(0xFF9E9E9E) // Material Grey 500 — visible in both light and dark
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

    val topMerchantInsight: StateFlow<String?> = topMerchants.map { list ->
        list.firstOrNull()?.let {
            val amount = com.finsave.core.common.formatter.IndianNumberFormatter.format(it.totalPaise, showPaise = false, showSymbol = true)
            "You spent $amount across ${it.txCount} orders on ${it.name} this month"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    // ── E2.5: Calendar Heatmap ────────────────────────────────
    
    val dailySpendMap: StateFlow<Map<LocalDate, Long>> = run {
        val today = LocalDate.now(istZone)
        val startDate = today.minusDays(83) // 84 days total (12 weeks)
        
        transactionRepository.getTransactionsByDateRange(startDate, today)
            .map { transactions ->
                transactions
                    .asSequence()
                    .filter { it.type == TransactionType.DEBIT }
                    .groupBy { it.date }
                    .mapValues { (_, dayTransactions) -> dayTransactions.sumOf { it.amountPaise } }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    }

    private val _selectedCalendarDate = MutableStateFlow<LocalDate?>(null)
    val selectedCalendarDate: StateFlow<LocalDate?> = _selectedCalendarDate.asStateFlow()
    
    val selectedDayTransactions: StateFlow<List<com.finsave.domain.model.Transaction>> = _selectedCalendarDate
        .flatMapLatest { date ->
            if (date == null) {
                flowOf(emptyList())
            } else {
                transactionRepository.getTransactionsByDateRange(date, date)
                    .map { transactions ->
                        transactions.sortedByDescending { it.date } // Just sort, don't filter to only DEBIT so they can see all activity
                    }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onCalendarDaySelected(date: LocalDate) {
        _selectedCalendarDate.value = date
    }
    
    val useIndianNumberSystem = MutableStateFlow(
        preferencesManager.getBoolean(com.finsave.core.common.Constants.PREFS_USE_INDIAN_NUMBER_SYSTEM, true)
    ).asStateFlow()

    // ── E2.2: Spend DNA Personality Card ────────────────────────

    private val monthDisplayFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    val spendDnaData: StateFlow<SpendDnaData?> = combine(
        segments,
        totalSpendPaise,
        dayOfWeekSpend,
        topMerchants,
        budgetRepository.getActiveBudgets()
    ) { segs, totalSpent, dowSpend, merchants, budgets ->
        if (segs.isEmpty() || totalSpent == 0L) return@combine null

        val topSeg = segs.first()
        val personality = classifyPersonality(
            segments = segs,
            totalSpent = totalSpent,
            dayOfWeekSpend = dowSpend,
            topMerchants = merchants,
            totalBudgetPaise = budgets.sumOf { it.limitAmountPaise }
        )

        val totalBudget = budgets.sumOf { it.limitAmountPaise }
        val savingsPct = if (totalBudget > 0) {
            ((totalBudget - totalSpent).coerceAtLeast(0) * 100 / totalBudget).toInt()
        } else 0

        SpendDnaData(
            topCategory = topSeg.category.name,
            topCategoryEmoji = topSeg.category.emoji,
            topCategoryAmount = topSeg.amountPaise,
            totalSpent = totalSpent,
            savingsPercent = savingsPct,
            personalityType = personality,
            month = currentMonth.format(monthDisplayFormatter)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _shareUri = MutableSharedFlow<Uri>(extraBufferCapacity = 1)
    val shareUri: SharedFlow<Uri> = _shareUri.asSharedFlow()

    /**
     * Generates the Spend DNA bitmap, saves to cache, and emits a share URI.
     */
    fun onShareSpendDna(context: Context) {
        val data = spendDnaData.value ?: return
        viewModelScope.launch {
            val uri = withContext(Dispatchers.Default) {
                // Clean up old files
                context.cacheDir.listFiles()?.filter {
                    it.name.startsWith("spend_dna_") && it.name.endsWith(".png")
                }?.forEach { it.delete() }

                // Generate bitmap
                val bitmap = SpendDnaGenerator.generate(data)

                // Save to cache
                val file = File(context.cacheDir, "spend_dna_${System.currentTimeMillis()}.png")
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()

                // Get FileProvider URI
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
            }
            _shareUri.tryEmit(uri)
        }
    }

    private fun classifyPersonality(
        segments: List<InsightSegment>,
        totalSpent: Long,
        dayOfWeekSpend: List<DayOfWeekRow>,
        topMerchants: List<TopMerchantRow>,
        totalBudgetPaise: Long
    ): PersonalityType {
        // Weekend Warrior: Sat+Sun > 60% of weekly spend
        if (dayOfWeekSpend.isNotEmpty()) {
            val weekTotal = dayOfWeekSpend.sumOf { it.totalPaise }
            val weekendTotal = dayOfWeekSpend
                .filter { it.dayLabel == "Sat" || it.dayLabel == "Sun" }
                .sumOf { it.totalPaise }
            if (weekTotal > 0 && weekendTotal.toFloat() / weekTotal > 0.60f) {
                return PersonalityType.WEEKEND_WARRIOR
            }
        }

        // Food Lover: Food & Dining is top AND > 40%
        val topCategory = segments.firstOrNull()
        if (topCategory != null) {
            val isFoodTop = topCategory.category.name.contains("Food", ignoreCase = true)
                    || topCategory.category.name.contains("Dining", ignoreCase = true)
            if (isFoodTop && totalSpent > 0 && topCategory.amountPaise.toFloat() / totalSpent > 0.40f) {
                return PersonalityType.FOOD_LOVER
            }
        }

        // Smart Saver: actual spend < 70% of total budget
        if (totalBudgetPaise > 0 && totalSpent.toFloat() / totalBudgetPaise < 0.70f) {
            return PersonalityType.SMART_SAVER
        }

        // Subscription Hoarder: Subscriptions category with >= 5 distinct merchants
        val subMerchants = topMerchants.filter { merchant ->
            segments.any { seg ->
                seg.category.name.contains("Subscription", ignoreCase = true)
            }
        }
        if (subMerchants.size >= 5) {
            return PersonalityType.SUBSCRIPTION_HOARDER
        }

        // Default
        return PersonalityType.DAILY_DRIVER
    }
}
