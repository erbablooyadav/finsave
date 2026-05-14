package com.finsave.data.repository

import com.finsave.data.local.dao.AnalyticsDao
import com.finsave.data.local.entity.AnalyticsEventEntity
import com.finsave.domain.repository.AnalyticsEvent
import com.finsave.domain.repository.AnalyticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.google.gson.Gson

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val gson: Gson
) : AnalyticsRepository {

    override suspend fun trackEvent(name: String, properties: Map<String, Any>?) {
        val json = properties?.let { gson.toJson(it) }
        analyticsDao.insertEvent(
            AnalyticsEventEntity(
                eventName = name,
                propertiesJson = json
            )
        )
    }

    override fun getRecentEvents(limit: Int): Flow<List<AnalyticsEvent>> {
        return analyticsDao.getRecentEvents(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun clearAnalytics() {
        analyticsDao.deleteAllEvents()
    }

    private fun AnalyticsEventEntity.toDomainModel(): AnalyticsEvent {
        return AnalyticsEvent(
            id = id,
            name = eventName,
            propertiesJson = propertiesJson,
            timestamp = timestamp
        )
    }
}
