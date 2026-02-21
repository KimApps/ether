package com.example.error_logger.di

import com.example.error_logger.ErrorLoggerService
import com.example.error_logger.LogcatErrorLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorLoggerModule {

    @Binds
    @Singleton
    abstract fun bindErrorLoggerService(
        impl: LogcatErrorLogger
    ): ErrorLoggerService
}