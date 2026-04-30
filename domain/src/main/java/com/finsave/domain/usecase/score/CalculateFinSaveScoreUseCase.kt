package com.finsave.domain.usecase.score

import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Use case: Calculate FinSave Score.
 *
 * Formula: score = clamp(round((streakDays × 2) + (budgetAdherencePercent × 0.5)), 0, 100)
 *
 * Requirements: 13.5
 */
class CalculateFinSaveScoreUseCase @Inject constructor() {
    
    /**
     * Calculates the FinSave Score based on budget streak and adherence.
     *
     * @param streakDays Number of consecutive days the user has stayed within their total budget
     * @param budgetAdherencePercent Percentage of active budgets currently under their limit (0-100)
     * @return Score in range [0, 100]
     */
    operator fun invoke(streakDays: Int, budgetAdherencePercent: Float): Int {
        val raw = (streakDays * 2) + (budgetAdherencePercent * 0.5f)
        return raw.roundToInt().coerceIn(0, 100)
    }
}
