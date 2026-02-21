package com.kimapps.signing.di

import android.content.Context
import com.kimapps.signing.layer.data.repository.SigningRepositoryImpl
import com.kimapps.signing.layer.domain.repository.SigningRepository
import com.kimapps.signing.wallet_connect.WalletConnectInitializer
import com.kimapps.signing.wallet_connect.WalletConnectManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    companion object {

        @Provides
        @Singleton
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideWalletConnectManager(
            applicationScope: CoroutineScope
        ): WalletConnectManager = WalletConnectManager(applicationScope)

        @Provides
        @Singleton
        fun provideSigningInitializer(
            @ApplicationContext context: Context, // Hilt provides this automatically
            walletConnectManager: WalletConnectManager
        ): WalletConnectInitializer {
            return WalletConnectInitializer(context, walletConnectManager)
        }
    }
}