package com.finsave.core.common.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.finsave.core.common.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.finsaveReactivePrefsDataStore by preferencesDataStore(
    name = "finsave_reactive_prefs",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = Constants.PREFS_NAME,
                keysToMigrate = setOf(
                    Constants.PREFS_FINSAVE_SCORE,
                    Constants.PREFS_BUDGET_STREAK_COUNT
                )
            )
        )
    }
)

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideReactivePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.finsaveReactivePrefsDataStore
}
