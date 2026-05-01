package com.finsave.domain.usecase.transaction

import com.finsave.domain.model.TransactionType
import com.finsave.domain.repository.MerchantCatalogRepository
import com.finsave.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetMerchantSuggestionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantCatalogRepository: MerchantCatalogRepository
) {
    operator fun invoke(query: String): Flow<List<String>> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.length < 2) return flowOf(emptyList())

        val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
        val startDate = today.minusDays(90)

        return flow {
            val bundledMerchants = merchantCatalogRepository.getBundledMerchantNames()
            emitAll(
                transactionRepository.getTransactionsByDateRange(startDate, today)
                    .map { transactions ->
                        val recentMatches = transactions
                            .asSequence()
                            .filter { it.type == TransactionType.DEBIT }
                            .map { it.merchantName.trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                            .filter { it.startsWith(normalizedQuery, ignoreCase = true) }
                            .toList()

                        val recentKeys = recentMatches.mapTo(mutableSetOf()) { it.lowercase() }
                        val bundledMatches = bundledMerchants
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() }
                            .filter { it.startsWith(normalizedQuery, ignoreCase = true) }
                            .filter { it.lowercase() !in recentKeys }
                            .sortedWith(String.CASE_INSENSITIVE_ORDER)
                            .toList()

                        (recentMatches + bundledMatches)
                            .distinctBy { it.lowercase() }
                            .take(5)
                    }
            )
        }
    }
}
