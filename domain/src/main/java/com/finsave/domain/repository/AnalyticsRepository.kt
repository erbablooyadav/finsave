package com.finsave.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * AnalyticsRepository handles local-only event tracking.
 */
interface AnalyticsRepository {
    
    /**
     * Records an event with optional properties.
     */
    suspend fun trackEvent(name: String, properties: Map<String, Any>? = null)
    
    /**
     * Retrieves recent events for reporting.
     */
    fun getRecentEvents(limit: Int): Flow<List<AnalyticsEvent>>
    
    /**
     * Clears all recorded events.
     */
    suspend fun clearAnalytics()
}

data class AnalyticsEvent(
    val id: Long,
    val name: String,
    val propertiesJson: String?,
    val timestamp: Long
)
