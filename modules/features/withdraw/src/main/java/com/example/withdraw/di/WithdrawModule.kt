package com.example.withdraw.di

import com.example.withdraw.layer.data.repository.WithdrawRepositoryImpl
import com.example.withdraw.layer.domain.repository.WithdrawRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WithdrawModule {
    /**
     * Binds withdraw repository
     */
    @Binds
    @Singleton
    abstract fun bindWithdrawRepository(withdrawRepositoryImpl: WithdrawRepositoryImpl)
            : WithdrawRepository
}
