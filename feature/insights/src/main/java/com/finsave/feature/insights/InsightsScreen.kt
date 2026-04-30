package com.finsave.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
                                text = com.finsave.core.common.formatter.IndianNumberFormatter.format(
                                    paiseAmount = totalSpendPaise,
                                    useIndianSystem = useIndianSystem,
                                    showPaise = false,
                                    showSymbol = true
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
            text = com.finsave.core.common.formatter.IndianNumberFormatter.format(
                paiseAmount = segment.amountPaise,
                useIndianSystem = useIndianSystem,
                showPaise = false,
                showSymbol = true
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
