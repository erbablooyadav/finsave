package com.finsave.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.finsave.core.ui.theme.LocalSpacing

/**
 * ShimmerBox — Base shimmer composable using infiniteTransition.
 *
 * Animates alpha between 0.3f and 1.0f with a 900ms tween in reverse mode,
 * creating a pulsing shimmer effect. Uses MaterialTheme.colorScheme.surfaceVariant
 * for full dark-mode safety.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val shimmer by rememberInfiniteTransition(label = "shimmer")
        .animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = shimmer))
    )
}

/**
 * DashboardSkeleton — Mimics the Dashboard header card + 3 transaction rows.
 */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Header card skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
        )

        // Score card skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // Section title skeleton
        ShimmerBox(
            modifier = Modifier
                .width(140.dp)
                .height(16.dp)
        )

        // 3 transaction row skeletons
        repeat(3) {
            TransactionRowSkeleton()
        }
    }
}

/**
 * TransactionsSkeleton — Mimics a list of 5 transaction rows.
 */
@Composable
fun TransactionsSkeleton(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Search bar skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Filter chips skeleton
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.small))

        // Month header skeleton
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        // 5 transaction row skeletons
        repeat(5) {
            TransactionRowSkeleton()
        }
    }
}

/**
 * BudgetSkeleton — Mimics 3 budget progress cards.
 */
@Composable
fun BudgetSkeleton(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Header area
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        // 3 budget card skeletons
        repeat(3) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(16.dp)
                    )
                }
                // Progress bar skeleton
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(12.dp)
                )
            }
        }
    }
}

/**
 * InsightsSkeleton — Mimics pie chart placeholder + 3 category rows.
 */
@Composable
fun InsightsSkeleton(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie chart placeholder (circle)
        ShimmerBox(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(spacing.small))

        // Summary row skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(60.dp)
                            .height(12.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(20.dp)
                    )
                }
            }
        }

        // 3 category spend rows
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(14.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp)
                )
            }
        }
    }
}

/**
 * Shared transaction row skeleton — used by both Dashboard and Transactions skeletons.
 */
@Composable
private fun TransactionRowSkeleton() {
    val spacing = LocalSpacing.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Avatar circle
        ShimmerBox(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )

        // Name + date
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(10.dp)
            )
        }

        // Amount
        ShimmerBox(
            modifier = Modifier
                .width(70.dp)
                .height(16.dp)
        )
    }
}
