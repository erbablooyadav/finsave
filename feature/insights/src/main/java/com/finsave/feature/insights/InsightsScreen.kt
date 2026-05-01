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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.finsave.core.ui.theme.LocalSpacing

@Composable
fun InsightsScreen(
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val totalSpendPaise by viewModel.totalSpendPaise.collectAsState()
    val segments by viewModel.segments.collectAsState()
    val monthlyTrend by viewModel.monthlyTrend.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val dayOfWeekSpend by viewModel.dayOfWeekSpend.collectAsState()
    val useIndianSystem by viewModel.useIndianNumberSystem.collectAsState()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
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
                        .padding(vertical = spacing.large),
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
                    Text(
                        text = "No spending data available this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    useIndianSystem = useIndianSystem
                )
            }

            item {
                DayOfWeekSection(
                    dayOfWeekSpend = dayOfWeekSpend,
                    useIndianSystem = useIndianSystem
                )
            }
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
    val maxSpend = monthlyTrend.maxOfOrNull { it.debitPaise } ?: 0L

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyTrend.forEachIndexed { index, point ->
                    val fraction = barFraction(point.debitPaise, maxSpend)
                    val barColor = if (index == monthlyTrend.lastIndex) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = IndianNumberFormatter.formatCompact(point.debitPaise),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.semantics {
                                contentDescription = "${point.monthLabel} spending ${IndianNumberFormatter.formatForAccessibility(point.debitPaise)}"
                            }
                        )
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(104.dp),
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
                            text = point.monthLabel,
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

@Composable
private fun TopMerchantsSection(
    topMerchants: List<TopMerchantRow>,
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
            topMerchants.forEach { merchant ->
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
                    .height(156.dp),
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
