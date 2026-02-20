package com.kimapps.home.di

import com.kimapps.home.home.data.repository.HomeRepositoryImpl
import com.kimapps.home.home.domain.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {
    /**
     * Binds the interface HomeRepository to its implementation HomeRepositoryImpl.
     * Hilt will use HomeRepositoryImpl whenever HomeRepository is required.
     */
    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository

}