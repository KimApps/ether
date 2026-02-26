package com.kimapps.network.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication token storage and retrieval using Jetpack DataStore.
 * DataStore is the recommended replacement for SharedPreferences, providing:
 * - Type safety with Kotlin Flow
 * - Asynchronous API (no UI blocking)
 * - Data consistency guarantees
 * - Support for coroutines
 *
 * All operations are suspend functions or return Flows for reactive updates.
 */
@Singleton
class TokenManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    /**
     * Saves both authentication and refresh tokens asynchronously.
     * This is a suspend function - call from coroutine or ViewModel scope.
     *
     * @param token The JWT or auth token to store.
     * @param refreshToken The refresh token to store.
     *
     * Example:
     * ```
     * viewModelScope.launch {
     *     tokenManager.saveTokens("eyJhbGc...", "refreshToken123")
     * }
     * ```
     */
    suspend fun saveTokens(token: String, refreshToken: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = token
            preferences[KEY_REFRESH_TOKEN] = refreshToken
        }
    }

    /**
     * Retrieves the current authentication token asynchronously.
     * This is a suspend function - call from coroutine or ViewModel scope.
     *
     * @return The stored token, or null if no token exists.
     *
     * Example:
     * ```
     * viewModelScope.launch {
     *     val token = tokenManager.getToken()
     *     if (token != null) {
     *         // User is logged in
     *     }
     * }
     * ```
     */
    suspend fun getToken(): String? {
        return dataStore.data
            .map { preferences -> preferences[KEY_AUTH_TOKEN] }
            .firstOrNull()
    }

    /**
     * Retrieves the current refresh token asynchronously.
     * This is a suspend function - call from coroutine or ViewModel scope.
     *
     * @return The stored refresh token, or null if no token exists.
     */

    suspend fun getRefreshToken(): String? {
        return dataStore.data
            .map { preferences -> preferences[KEY_REFRESH_TOKEN] }
            .firstOrNull()
    }

    /**
     * Returns a Flow of the authentication token for reactive updates.
     * Use this when you want to observe token changes in real-time.
     *
     * @return Flow that emits the token whenever it changes, or null if no token.
     *
     * Example:
     * ```
     * tokenManager.getTokenFlow()
     *     .collect { token ->
     *         if (token != null) {
     *             // User is logged in
     *         } else {
     *             // User is logged out
     *         }
     *     }
     * ```
     */
    fun getTokenFlow(): Flow<String?> {
        return dataStore.data.map { preferences ->
            preferences[KEY_AUTH_TOKEN]
        }
    }

    /**
     * Clears the stored authentication token asynchronously (e.g., on logout).
     * This is a suspend function - call from coroutine or ViewModel scope.
     *
     * Example:
     * ```
     * viewModelScope.launch {
     *     tokenManager.clearToken()
     *     // Navigate to login screen
     * }
     * ```
     */
    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(KEY_AUTH_TOKEN)
            preferences.remove(KEY_REFRESH_TOKEN)
        }
    }

    /**
     * Checks if a valid token exists asynchronously.
     * This is a suspend function - call from coroutine or ViewModel scope.
     *
     * @return true if a token is stored, false otherwise.
     *
     * Example:
     * ```
     * viewModelScope.launch {
     *     if (tokenManager.hasToken()) {
     *         // Navigate to home
     *     } else {
     *         // Show login
     *     }
     * }
     * ```
     */
    suspend fun hasToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * Synchronous (blocking) version of getToken() for use in interceptors.
     * This uses runBlocking internally - should only be used in network interceptors
     * where coroutines cannot be used.
     *
     * ⚠️ WARNING: This blocks the calling thread. Prefer using the suspend version
     * in ViewModels and repositories.
     *
     * @return The stored token, or null if no token exists.
     */
    fun getTokenBlocking(): String? {
        return kotlinx.coroutines.runBlocking {
            getToken()
        }
    }

    /**
     * Synchronous (blocking) version of getRefreshToken() for use in interceptors.
     * This uses runBlocking internally - should only be used in network interceptors
     * where coroutines cannot be used.
     *
     * ⚠️ WARNING: This blocks the calling thread. Prefer using the suspend version
     * in ViewModels and repositories.
     *
     * @return The stored refresh token, or null if no token exists.
     */
    fun getRefreshTokenBlocking(): String? {
        return kotlinx.coroutines.runBlocking {
            getRefreshToken()
        }
    }

    /**
     * Synchronous (blocking) version of saveTokens() for use in interceptors.
     * This uses runBlocking internally - should only be used in network interceptors
     * where coroutines cannot be used.
     *
     * ⚠️ WARNING: This blocks the calling thread. Prefer using the suspend version
     * in ViewModels and repositories.
     */
    fun saveTokensBlocking(token: String, refreshToken: String) {
        kotlinx.coroutines.runBlocking {
            saveTokens(token, refreshToken)
        }
    }

    /**
     * Synchronous (blocking) version of clearToken() for use in interceptors.
     * This uses runBlocking internally - should only be used in network interceptors
     * where coroutines cannot be used.
     *
     * ⚠️ WARNING: This blocks the calling thread. Prefer using the suspend version
     * in ViewModels and repositories.
     */
    fun clearTokensBlocking() {
        kotlinx.coroutines.runBlocking {
            clearTokens()
        }
    }
}

