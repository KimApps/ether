package com.kimapps.localstorage.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent key-value store that transparently encrypts every value before
 * writing and decrypts it on read.
 *
 * **Storage layer** — Jetpack [DataStore] with [Preferences]: asynchronous,
 * coroutine-friendly, and free from the consistency bugs of SharedPreferences.
 *
 * **Encryption layer** — [SecurityManager] wraps Google Tink (AES-256-GCM)
 * backed by the Android Keystore, so the raw key material never leaves secure
 * hardware. Only the Base64-encoded ciphertext is written to DataStore.
 *
 * All three methods are `suspend` because DataStore I/O is asynchronous.
 * Call them from a coroutine scope (e.g. `viewModelScope` or a repository
 * injected with an `IO` dispatcher).
 */
@Singleton
class EncryptedStorage @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val securityManager: SecurityManager
) {
    /**
     * Encrypts [value] with [SecurityManager] and persists the ciphertext
     * under [key] in DataStore. Any existing value for [key] is overwritten.
     */
    suspend fun saveString(key: String, value: String) {
        val encryptedValue = securityManager.encrypt(value)
        dataStore.edit { prefs ->
            // stringPreferencesKey scopes the key to the String type,
            // preventing accidental type collisions with other preference keys.
            prefs[stringPreferencesKey(key)] = encryptedValue
        }
    }

    /**
     * Reads the ciphertext stored under [key] and decrypts it.
     *
     * Returns `null` in two cases:
     * - The key has never been written.
     * - Decryption fails (e.g. the key was written by a different keyset).
     *   The exception is swallowed and `null` is returned so callers treat a
     *   corrupt or missing value the same way — as "no value".
     */
    suspend fun getString(key: String): String? {
        // .first() collects exactly one emission from the DataStore Flow and
        // then cancels the upstream — equivalent to a one-shot async read.
        val encryptedValue = dataStore.data.map { prefs ->
            prefs[stringPreferencesKey(key)]
        }.first() ?: return null

        return try {
            securityManager.decrypt(encryptedValue)
        } catch (e: Exception) {
            // Decryption can fail if the stored ciphertext is corrupt or was
            // encrypted with a different keyset (e.g. after a backup restore).
            // Return null so callers handle it as a missing value.
            null
        }
    }

    /**
     * Removes the entry for [key] from DataStore.
     * If the key does not exist, this is a no-op.
     */
    suspend fun remove(key: String) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
    }
}