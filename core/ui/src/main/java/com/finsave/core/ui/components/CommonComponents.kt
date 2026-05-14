package com.finsave.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finsave.core.common.formatter.IndianNumberFormatter
import com.finsave.core.ui.theme.CreditGreen
import com.finsave.core.ui.theme.CreditGreenDark
import com.finsave.core.ui.theme.CreditGreenLight
import com.finsave.core.ui.theme.DebitRed
import com.finsave.core.ui.theme.DebitRedDark
import com.finsave.core.ui.theme.DebitRedLight
import com.finsave.core.ui.theme.LocalSpacing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing


/**
 * FinSave Card — Elevated card with consistent styling.
 *
 * Uses Material 3 card with FinSave-specific elevation and shape.
 * Supports optional gradient background for premium cards.
 */
@Composable
fun FinSaveCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 4.dp
            )
        ) {
            Box(modifier = Modifier.padding(spacing.medium)) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(modifier = Modifier.padding(spacing.medium)) {
                content()
            }
        }
    }
}

/**
 * Amount Display — Renders INR amounts with color coding.
 *
 * - Debit amounts shown in red with "-" prefix
 * - Credit amounts shown in green with "+" prefix
 * - Uses Indian number formatting by default
 * - Includes TalkBack accessibility description
 */
@Composable
fun AmountDisplay(
    amountPaise: Long,
    isDebit: Boolean,
    modifier: Modifier = Modifier,
    useIndianSystem: Boolean = true,
    showPaise: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    val color by animateColorAsState(
        targetValue = when {
            isDebit && isDark -> DebitRedDark
            isDebit -> DebitRed
            isDark -> CreditGreenDark
            else -> CreditGreen
        },
        animationSpec = tween(300),
        label = "amountColor"
    )

    val prefix = if (isDebit) "- " else "+ "
    val formattedAmount = IndianNumberFormatter.format(
        paiseAmount = kotlin.math.abs(amountPaise),
        useIndianSystem = useIndianSystem,
        showPaise = showPaise,
        showSymbol = true
    )
    val displayText = "$prefix$formattedAmount"
    val accessibilityText = IndianNumberFormatter.formatForAccessibility(
        if (isDebit) -kotlin.math.abs(amountPaise) else kotlin.math.abs(amountPaise)
    )

    Text(
        text = displayText,
        style = style.copy(
            fontWeight = FontWeight.SemiBold,
            color = color
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics {
            contentDescription = if (isDebit) "Spent $accessibilityText" else "Received $accessibilityText"
        }
    )
}

/**
 * Category Chip — Emoji + label chip for expense categories.
 */
@Composable
fun CategoryChip(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    categoryColor: Color = MaterialTheme.colorScheme.primary
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            categoryColor.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(200),
        label = "chipBg"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) categoryColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chipText"
    )

    val spacing = LocalSpacing.current

    androidx.compose.material3.SuggestionChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier.semantics {
            contentDescription = "$label category${if (isSelected) ", selected" else ""}"
        },
        shape = MaterialTheme.shapes.small
    )
}

/**
 * Transaction Type Indicator — Small colored dot showing debit/credit.
 */
@Composable
fun TransactionTypeIndicator(
    isDebit: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val bgColor = when {
        isDebit && isDark -> DebitRedDark
        isDebit -> DebitRed
        isDark -> CreditGreenDark
        else -> CreditGreen
    }

    Box(
        modifier = modifier
            .size(8.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bgColor)
    )
}

/**
 * Gradient Header Card — Used for dashboard header with spend summary.
 */
@Composable
fun GradientHeaderCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary
    ),
    content: @Composable () -> Unit
) {
    val spacing = LocalSpacing.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.linearGradient(colors = gradientColors)
            )
            .padding(spacing.large)
    ) {
        content()
    }
}

/**
 * Animated Spend Header Card — Dashboard hero card with live counter and budget ring.
 *
 * Features:
 * - Gradient background (primary → primaryContainer)
 * - Animated rupee counter that morphs on data change
 * - Canvas-drawn budget ring with color-coded fill (green/amber/red)
 * - Smart "₹X/day left" banner when budget is set
 * - Full TalkBack accessibility
 */
@Composable
fun AnimatedSpendHeaderCard(
    totalSpentPaise: Long,
    totalBudgetPaise: Long,
    daysRemaining: Int,
    hasTotalBudget: Boolean,
    useIndianSystem: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    // Animated rupee counter using Animatable
    val animatedSpent = remember { Animatable(0f) }
    LaunchedEffect(totalSpentPaise) {
        animatedSpent.animateTo(
            targetValue = totalSpentPaise.toFloat(),
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutSlowInEasing
            )
        )
    }

    // Budget percentage (derived to prevent unnecessary recomposition)
    val percentUsed by remember(totalSpentPaise, totalBudgetPaise, hasTotalBudget) {
        derivedStateOf {
            if (hasTotalBudget && totalBudgetPaise > 0) {
                totalSpentPaise.toFloat() / totalBudgetPaise.toFloat()
            } else 0f
        }
    }

    // Animated ring fill
    val animatedPercent by animateFloatAsState(
        targetValue = if (hasTotalBudget) percentUsed.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(600),
        label = "budgetRingPercent"
    )

    // Ring color: green <70%, amber 70–99%, red ≥100%
    val ringColor by animateColorAsState(
        targetValue = when {
            percentUsed >= 1f -> MaterialTheme.colorScheme.error
            percentUsed >= 0.7f -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(600),
        label = "ringColor"
    )

    // Daily budget remaining
    val dailyBudgetPaise by remember(totalBudgetPaise, totalSpentPaise, daysRemaining, hasTotalBudget) {
        derivedStateOf {
            if (hasTotalBudget && daysRemaining > 0 && totalBudgetPaise > totalSpentPaise) {
                (totalBudgetPaise - totalSpentPaise) / daysRemaining
            } else 0L
        }
    }

    // Formatted spent amount from animated value
    val formattedSpent = IndianNumberFormatter.format(
        paiseAmount = animatedSpent.value.toLong(),
        useIndianSystem = useIndianSystem,
        showPaise = true,
        showSymbol = true
    )

    // TalkBack accessibility description
    val accessibilityText = buildString {
        append("Total spent: ${IndianNumberFormatter.formatForAccessibility(totalSpentPaise)}")
        if (hasTotalBudget) {
            append(", ${(percentUsed * 100).toInt()} percent of budget used")
            append(", $daysRemaining days remaining")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(spacing.large)
            .semantics { contentDescription = accessibilityText }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spent this month",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(spacing.extraSmall))
                    Text(
                        text = formattedSpent,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Budget ring — only visible when budget exists
                if (hasTotalBudget) {
                    BudgetRing(
                        percent = animatedPercent,
                        ringColor = ringColor,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                        textColor = MaterialTheme.colorScheme.onPrimary,
                        percentUsed = percentUsed,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Smart "₹X/day left" banner
            if (hasTotalBudget && daysRemaining > 0) {
                Spacer(modifier = Modifier.height(spacing.medium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                        .padding(spacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(spacing.small))
                    val dailyFormatted = IndianNumberFormatter.format(
                        paiseAmount = dailyBudgetPaise,
                        useIndianSystem = useIndianSystem,
                        showPaise = false,
                        showSymbol = true
                    )
                    Text(
                        text = "$daysRemaining days left \u00B7 $dailyFormatted/day remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Budget Ring — Canvas-drawn circular arc with percentage text overlay.
 */
@Composable
private fun BudgetRing(
    percent: Float,
    ringColor: Color,
    trackColor: Color,
    textColor: Color,
    percentUsed: Float,
    modifier: Modifier = Modifier
) {
    val strokeWidth = 8.dp
    val percentText = "${(percentUsed.coerceAtMost(9.99f) * 100).toInt()}%"

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension
            val topLeft = Offset(
                (size.width - diameter + stroke) / 2f,
                (size.height - diameter + stroke) / 2f
            )
            val arcSize = Size(diameter - stroke, diameter - stroke)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )

            // Foreground fill
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = percent.coerceAtMost(1f) * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        }

        Text(
            text = percentText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// Helper: Get luminance of a Color for dark mode detection
private fun Color.luminance(): Float {
    val r = red * 0.2126f
    val g = green * 0.7152f
    val b = blue * 0.0722f
    return r + g + b
}

@Composable
fun StreakCard(
    streakCount: Int,
    bestStreak: Int,
    isStreakBroken: Boolean,
    score: Int, // Included for E3.2 compatibility
    modifier: Modifier = Modifier
) {
    val containerColor = if (isStreakBroken) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val streakDescription = if (isStreakBroken) "Streak lost! Start again today" else "$streakCount day streak. Best streak $bestStreak days."

    FinSaveCard(
        modifier = modifier.fillMaxWidth().semantics { contentDescription = streakDescription },
        containerColor = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                if (isStreakBroken) {
                    Text(
                        text = "😢 Streak lost! Start again today",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (streakCount >= 3) {
                            val infiniteTransition = rememberInfiniteTransition()
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.15f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                )
                            )
                            Text(
                                text = "🔥",
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                            )
                        } else {
                            Text(
                                text = "🔥",
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                        Text(
                            text = " $streakCount",
                            style = MaterialTheme.typography.displayLarge
                        )
                    }
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bestStreak > 0) {
                        Text(
                            text = "Best: $bestStreak days",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            
            // FinSave Score (E3.2 score ring)
            FinSaveScoreRing(score = score)
        }
    }
}

@Composable
fun FinSaveScoreRing(
    score: Int,
    modifier: Modifier = Modifier
) {
    val animatedScore by androidx.compose.animation.core.animateIntAsState(
        targetValue = score,
        animationSpec = tween(1500)
    )

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val ringColor = when {
        score < 40 -> MaterialTheme.colorScheme.error
        score < 70 -> MaterialTheme.colorScheme.tertiary
        else -> if (isDark) CreditGreenDark else CreditGreen
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.runtime.LaunchedEffect(animatedScore) {
        if (animatedScore == score && score > 0) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    val shimmerAlpha by if (score < 40) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(1000),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(80.dp)
            .semantics { 
                contentDescription = "FinSave Score: $score out of 100" 
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            val stroke = 8.dp.toPx()
            val finalColor = ringColor.copy(alpha = shimmerAlpha)
            
            // Background circle
            drawCircle(
                color = ringColor.copy(alpha = 0.12f),
                style = Stroke(width = stroke)
            )
            
            // Foreground arc
            drawArc(
                color = finalColor,
                startAngle = -90f,
                sweepAngle = (animatedScore / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = animatedScore.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "score",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Info
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = tween(1500),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            )
        )
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { this.alpha = alpha },
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.semantics { contentDescription = message }
        )
    }
}


