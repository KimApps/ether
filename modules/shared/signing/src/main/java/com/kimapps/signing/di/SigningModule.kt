package com.kimapps.signing.di

import com.kimapps.signing.layer.data.repository.SigningRepositoryImpl
import com.kimapps.signing.layer.domain.repository.SigningRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SigningModule {
    /**
     * Binds the interface SigningRepository to its implementation SigningRepositoryImpl.
     * Hilt will use SigningRepositoryImpl whenever SigningRepository is required.
     */
    @Binds
    @Singleton
    abstract fun bindSigningRepository(
        signingRepositoryImpl: SigningRepositoryImpl
    ): SigningRepository
}