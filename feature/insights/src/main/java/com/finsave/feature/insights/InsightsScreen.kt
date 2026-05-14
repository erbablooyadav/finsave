package com.finsave.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.components.ChartSegment
import com.finsave.core.ui.components.CustomDonutChart
import com.finsave.core.ui.components.EmptyStateView
import com.finsave.core.ui.components.InsightsSkeleton
import com.finsave.core.ui.R
import com.finsave.core.ui.theme.LocalSpacing
import com.finsave.core.common.share.SpendDnaData
import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.text.style.TextOverflow
import com.finsave.core.ui.CalendarHeatmapChart
import com.finsave.domain.model.Transaction
import com.finsave.domain.model.TransactionType
import java.time.format.DateTimeFormatter

import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.common.fill

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val totalSpendPaise by viewModel.totalSpendPaise.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val topMerchantInsight by viewModel.topMerchantInsight.collectAsState()
    val dayOfWeekSpend by viewModel.dayOfWeekSpend.collectAsState()
    val useIndianSystem by viewModel.useIndianNumberSystem.collectAsState()
    val spendDna by viewModel.spendDnaData.collectAsState()
    val dailySpendMap by viewModel.dailySpendMap.collectAsState()
    val selectedDayTransactions by viewModel.selectedDayTransactions.collectAsState()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsState()
    var showDayDetails by remember { mutableStateOf(false) }
    
    val spacing = LocalSpacing.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // E2.2: Collect share URI and launch share intent
    LaunchedEffect(Unit) {
        viewModel.shareUri.collect { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Spend DNA"))
        }
    }

    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(segments) {
        if (segments.isNotEmpty()) isLoading = false
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(800)
        isLoading = false
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (isLoading) {
            InsightsSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = spacing.large),
            contentPadding = PaddingValues(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Text(
                    text = "Spending Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(spacing.medium))
            }

            item {
                // Chart
                val chartSegments = segments.map { 
                    ChartSegment(value = it.amountPaise.toFloat(), color = it.color) 
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.large)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Spending breakdown chart. Total spent: ${IndianNumberFormatter.formatForAccessibility(totalSpendPaise)}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CustomDonutChart(
                        segments = chartSegments,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        strokeWidth = 50f
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = IndianNumberFormatter.format(
                                    paiseAmount = totalSpendPaise,
                                    useIndianSystem = useIndianSystem,
                                    showPaise = false,
                                    showSymbol = true
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.semantics {
                                    contentDescription = IndianNumberFormatter.formatForAccessibility(totalSpendPaise)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = spacing.small)
                )
            }

            if (segments.isEmpty()) {
                item {
                    EmptyStateView(
                        vectorRes = R.drawable.ic_empty_transactions,
                        title = "No spending data",
                        body = "Add transactions to see your spending breakdown"
                    )
                }
            } else {
                items(segments, key = { it.category.id }) { segment ->
                    val percentage = if (totalSpendPaise > 0) (segment.amountPaise.toFloat() / totalSpendPaise) * 100 else 0f
                    InsightRow(
                        segment = segment,
                        percentage = percentage,
                        useIndianSystem = useIndianSystem
                    )
                }
            }

            item {
                MonthlySpendingSection(
                    monthlyTrend = monthlyTrend,
                    useIndianSystem = useIndianSystem
                )
            }

            item {
                TopMerchantsSection(
                    topMerchants = topMerchants,
                    insightCopy = topMerchantInsight,
                    useIndianSystem = useIndianSystem
                )
            }

            item {
                DayOfWeekSection(
                    dayOfWeekSpend = dayOfWeekSpend,
                    useIndianSystem = useIndianSystem
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = "Spending Heatmap",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    CalendarHeatmapChart(
                        dailySpend = dailySpendMap,
                        onDayClick = { date ->
                            viewModel.onCalendarDaySelected(date)
                            showDayDetails = true
                        }
                    )
                }
            }

            // E2.2: Spend DNA Personality Card
            if (spendDna != null) {
                item {
                    SpendDnaCard(
                        data = spendDna!!,
                        onShare = { viewModel.onShareSpendDna(context) }
                    )
                }
            }
        }
        } // else isLoading

        if (showDayDetails && selectedCalendarDate != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMM yyyy") }
            
            ModalBottomSheet(
                onDismissRequest = { showDayDetails = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.large)
                        .padding(bottom = spacing.large)
                ) {
                    Text(
                        text = selectedCalendarDate!!.format(dateFormatter),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = spacing.medium)
                    )
                    
                    if (selectedDayTransactions.isEmpty()) {
                        EmptyStateView(
                            vectorRes = R.drawable.ic_empty_transactions,
                            title = "No transactions",
                            body = "You didn't spend anything on this day."
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            items(selectedDayTransactions, key = { it.id }) { transaction ->
                                DayTransactionRowItem(
                                    transaction = transaction,
                                    useIndianSystem = useIndianSystem
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayTransactionRowItem(
    transaction: Transaction,
    useIndianSystem: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = transaction.merchantName.take(1).uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName.ifBlank { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = IndianNumberFormatter.format(
                    paiseAmount = transaction.amountPaise,
                    useIndianSystem = useIndianSystem,
                    showPaise = false,
                    showSymbol = true
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    TransactionType.DEBIT -> MaterialTheme.colorScheme.error
                    TransactionType.CREDIT -> MaterialTheme.colorScheme.secondary
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.semantics {
                    contentDescription = IndianNumberFormatter.formatForAccessibility(transaction.amountPaise)
                }
            )
        }
    }
}

@Composable
fun InsightRow(
    segment: InsightSegment,
    percentage: Float,
    useIndianSystem: Boolean
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(segment.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = segment.category.emoji, style = MaterialTheme.typography.titleLarge)
        }
        
        Spacer(modifier = Modifier.width(spacing.medium))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = segment.category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${"%.1f".format(percentage)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = IndianNumberFormatter.format(
                paiseAmount = segment.amountPaise,
                useIndianSystem = useIndianSystem,
                showPaise = false,
                showSymbol = true
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = IndianNumberFormatter.formatForAccessibility(segment.amountPaise)
            }
        )
    }
}

@Composable
private fun MonthlySpendingSection(
    monthlyTrend: List<MonthlyTrendPoint>,
    useIndianSystem: Boolean
) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Text(
            text = "Monthly Spending",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (monthlyTrend.isEmpty()) {
            Text(
                text = "No trend data available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val model = remember(monthlyTrend) {
                CartesianChartModel(
                    ColumnCartesianLayerModel.build {
                        series(monthlyTrend.map { it.debitPaise })
                    }
                )
            }

            val primary = MaterialTheme.colorScheme.primary
            val primaryContainer = MaterialTheme.colorScheme.primaryContainer

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                            monthlyTrend.mapIndexed { index, _ ->
                                rememberLineComponent(
                                    fill = fill(if (index == monthlyTrend.lastIndex) primary else primaryContainer),
                                    thickness = 24.dp,
                                    shape = com.patrykandpatrick.vico.core.common.shape.CorneredShape.rounded(topLeftPercent = 50, topRightPercent = 50)
                                )
                            }
                        )
                    ),
                    startAxis = VerticalAxis.rememberStart(
                        valueFormatter = { _, value, _ ->
                            IndianNumberFormatter.formatCompact(value.toLong())
                        }
                    ),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { _, value, _ ->
                            val index = value.toInt()
                            if (index in monthlyTrend.indices) monthlyTrend[index].monthLabel else ""
                        }
                    )
                ),
                model = model,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .semantics {
                        contentDescription = "Monthly spending trend chart showing ${monthlyTrend.size} months"
                    }
            )
        }
    }
}

@Composable
private fun TopMerchantsSection(
    topMerchants: List<TopMerchantRow>,
    insightCopy: String?,
    useIndianSystem: Boolean
) {
    val spacing = LocalSpacing.current

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text = "Top Merchants This Month",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (topMerchants.isEmpty()) {
            Text(
                text = "No merchant spending this month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (!insightCopy.isNullOrBlank()) {
                Text(
                    text = insightCopy,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = spacing.small)
                )
            }

            topMerchants.forEachIndexed { index, merchant ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (index == 0) "🏆" else "${index + 1}.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(32.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = merchant.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${merchant.txCount} transactions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = IndianNumberFormatter.format(
                                paiseAmount = merchant.totalPaise,
                                useIndianSystem = useIndianSystem,
                                showPaise = false,
                                showSymbol = true
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = "${merchant.name} ${IndianNumberFormatter.formatForAccessibility(merchant.totalPaise)}"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayOfWeekSection(
    dayOfWeekSpend: List<DayOfWeekRow>,
    useIndianSystem: Boolean
) {
    val spacing = LocalSpacing.current
    val maxSpend = dayOfWeekSpend.maxOfOrNull { it.totalPaise } ?: 0L
    val warningAmber = Color(0xFFFFB300)

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Text(
            text = "Spending by Day",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (dayOfWeekSpend.isEmpty()) {
            Text(
                text = "No weekday spending data available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Daily spending bar chart"
                    },
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                verticalAlignment = Alignment.Bottom
            ) {
                dayOfWeekSpend.forEach { day ->
                    val isHighest = maxSpend > 0 && day.totalPaise == maxSpend
                    val fraction = barFraction(day.totalPaise, maxSpend)
                    val barColor = if (isHighest) warningAmber else MaterialTheme.colorScheme.secondary

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = IndianNumberFormatter.formatCompact(day.totalPaise),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.semantics {
                                contentDescription = "${day.dayLabel} spending ${IndianNumberFormatter.formatForAccessibility(day.totalPaise)}"
                            }
                        )
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun barFraction(value: Long, maxValue: Long): Float {
    if (value <= 0L || maxValue <= 0L) return 0f
    return (value.toFloat() / maxValue.toFloat()).coerceIn(0.08f, 1f)
}

/**
 * Spend DNA Personality Card — Spotify Wrapped-style card.
 * Shows the user's spending personality for the current month with a Share button.
 * E2.2
 */
@Composable
private fun SpendDnaCard(
    data: SpendDnaData,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val gradientStart = Color(data.personalityType.gradientStart)
    val gradientEnd = Color(data.personalityType.gradientEnd)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(gradientStart, gradientEnd)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(spacing.large)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Title
                Text(
                    text = "My Spend DNA",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )

                // Personality emoji + name
                Text(
                    text = data.personalityType.emoji,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = data.personalityType.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Top category subtitle
                Text(
                    text = "${data.topCategoryEmoji} ${com.finsave.core.common.formatter.IndianNumberFormatter.format(data.topCategoryAmount)} on ${data.topCategory}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Stats row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = com.finsave.core.common.formatter.IndianNumberFormatter.format(data.totalSpent, showPaise = false),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Text("•", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.titleLarge)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${data.savingsPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                // Month
                Text(
                    text = data.month,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )

                // Share button
                androidx.compose.material3.FilledTonalButton(
                    onClick = onShare,
                    colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.semantics { contentDescription = "Share My Spend DNA" }
                ) {
                    Text("Share My Spend DNA")
                }
            }
        }
    }
}
