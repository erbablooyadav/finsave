package com.finsave.domain.usecase.settings

import com.finsave.domain.repository.DataResetRepository
import javax.inject.Inject

class ClearAllDataUseCase @Inject constructor(
    private val dataResetRepository: DataResetRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            dataResetRepository.clearAllData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
