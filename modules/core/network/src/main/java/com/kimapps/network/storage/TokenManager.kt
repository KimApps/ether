package com.kimapps.network.storage

import com.kimapps.localstorage.storage.EncryptedStorage
import com.kimapps.localstorage.storage.StorageConstants.KEY_AUTH_TOKEN
import com.kimapps.localstorage.storage.StorageConstants.KEY_REFRESH_TOKEN
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [EncryptedStorage] that scopes token storage to the
 * two well-known keys used for authentication.
 *
 * [EncryptedStorage] handles all encryption via Google Tink (AES-256-GCM)
 * backed by the Android Keystore — [TokenManager] has no knowledge of the
 * encryption details and simply delegates reads and writes by key.
 *
 * All primary operations are `suspend` because [EncryptedStorage] uses
 * Jetpack DataStore under the hood, which is asynchronous. The `Blocking`
 * variants exist solely for OkHttp interceptors, which run on a background
 * thread and cannot call suspend functions directly.
 */
@Singleton
class TokenManager @Inject constructor(
    private val storage: EncryptedStorage
) {

    /**
     * Encrypts and saves both tokens to DataStore via [EncryptedStorage].
     *
     * @param token The JWT access token to store.
     * @param refreshToken The refresh token to store.
     */
    suspend fun saveTokens(token: String, refreshToken: String) {
        storage.saveString(KEY_AUTH_TOKEN, token)
        storage.saveString(KEY_REFRESH_TOKEN, refreshToken)
    }

    /**
     * Returns the decrypted authentication token, or `null` if none is stored
     * or decryption fails.
     */
    suspend fun getToken(): String? = storage.getString(KEY_AUTH_TOKEN)

    /**
     * Returns the decrypted refresh token, or `null` if none is stored
     * or decryption fails.
     */
    suspend fun getRefreshToken(): String? = storage.getString(KEY_REFRESH_TOKEN)

    /**
     * Removes both tokens from DataStore (e.g. on logout or when a token
     * refresh fails and the session must be fully invalidated).
     */
    suspend fun clearTokens() {
        storage.remove(KEY_AUTH_TOKEN)
        storage.remove(KEY_REFRESH_TOKEN)
    }

    /**
     * Blocking variant of [getToken] for use inside OkHttp interceptors.
     *
     * OkHttp interceptors run on a dedicated background thread outside of
     * any coroutine scope — [runBlocking] bridges that gap. The call is
     * safe here because the interceptor thread is never the main thread.
     *
     * ⚠️ Do not call from a coroutine or the main thread; use [getToken] instead.
     */
    fun getTokenBlocking(): String? = runBlocking { getToken() }

    /**
     * Blocking variant of [getRefreshToken] for use inside OkHttp interceptors.
     *
     * ⚠️ Do not call from a coroutine or the main thread; use [getRefreshToken] instead.
     */
    fun getRefreshTokenBlocking(): String? = runBlocking { getRefreshToken() }

    /**
     * Blocking variant of [saveTokens] for use inside OkHttp interceptors
     * (e.g. persisting a new access token after a successful refresh).
     *
     * ⚠️ Do not call from a coroutine or the main thread; use [saveTokens] instead.
     */
    fun saveTokensBlocking(token: String, refreshToken: String) = runBlocking { saveTokens(token, refreshToken) }

    /**
     * Blocking variant of [clearTokens] for use inside OkHttp interceptors
     * (e.g. when a token refresh fails and the session must be invalidated).
     *
     * ⚠️ Do not call from a coroutine or the main thread; use [clearTokens] instead.
     */
    fun clearTokensBlocking() = runBlocking { clearTokens() }
}
