package com.kimapps.user.di

import androidx.test.espresso.core.internal.deps.dagger.Binds
import com.kimapps.user.layer.data.repository.UserRepositoryImpl
import com.kimapps.user.layer.domain.repository.UserRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UserModule {
    /**
     * Binds the interface UserRepository to its implementation UserRepositoryImpl.
     * Hilt will use UserRepositoryImpl whenever UserRepository is required.
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

}