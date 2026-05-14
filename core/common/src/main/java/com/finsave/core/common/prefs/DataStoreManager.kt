package com.finsave.core.common.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.finsave.core.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val finSaveScore: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey(Constants.PREFS_FINSAVE_SCORE)] ?: 0
    }

    val budgetStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey(Constants.PREFS_BUDGET_STREAK_COUNT)] ?: 0
    }

    val bestStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[intPreferencesKey(Constants.PREFS_BEST_BUDGET_STREAK_COUNT)] ?: 0
    }


    suspend fun saveScore(score: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(Constants.PREFS_FINSAVE_SCORE)] = score
        }
    }

    suspend fun saveStreak(streak: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(Constants.PREFS_BUDGET_STREAK_COUNT)] = streak
        }
    }

    suspend fun saveBestStreak(streak: Int) {
        dataStore.edit { prefs ->
            prefs[intPreferencesKey(Constants.PREFS_BEST_BUDGET_STREAK_COUNT)] = streak
        }
    }
}
