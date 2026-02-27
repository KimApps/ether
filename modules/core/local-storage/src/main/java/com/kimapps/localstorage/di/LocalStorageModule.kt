package com.kimapps.localstorage.di

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.kimapps.localstorage.storage.LocalStorageClient
import com.kimapps.localstorage.storage.StorageConstants.DATASTORE_NAME
import com.kimapps.localstorage.storage.StorageConstants.PREFS_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing local storage dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object LocalStorageModule {

    /**
     * Provides a singleton SharedPreferences instance.
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Provides a singleton DataStore<Preferences> instance.
     * By using PreferenceDataStoreFactory here, we keep the declaration 
     * local to the provider and avoid top-level properties.
     *
     * @param context Application context
     * @return DataStore instance for storing preferences
     */
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(DATASTORE_NAME) }
        )
    }

    /**
     * Provides a singleton LocalStorageClient instance.
     * Currently uses SharedPreferences as the underlying storage.
     *
     * @param sharedPreferences SharedPreferences instance
     * @return LocalStorageClient instance for local storage operations
     */
    @Provides
    @Singleton
    fun provideLocalStorageClient(sharedPreferences: SharedPreferences): LocalStorageClient {
        return LocalStorageClient(sharedPreferences)
    }
}
