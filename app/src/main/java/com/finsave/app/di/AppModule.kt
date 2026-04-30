package com.finsave.app.di

import com.finsave.app.sync.SyncManagerImpl
import com.finsave.domain.usecase.sync.SyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindSyncManager(
        impl: SyncManagerImpl
    ): SyncManager
}
