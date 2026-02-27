package com.kimapps.localstorage.storage

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles symmetric encryption and decryption using Google Tink's AES-256-GCM primitive.
 *
 * **Key storage** — the Tink keyset is stored in SharedPreferences under [prefName],
 * but the keyset itself is envelope-encrypted by the Android Keystore key identified
 * by [masterKeyUri]. The raw key material never leaves secure hardware.
 *
 * **Algorithm** — AES-256-GCM provides both confidentiality and integrity
 * (authenticated encryption). Any tampering with the ciphertext causes decryption
 * to throw rather than silently returning corrupted data.
 *
 * **Thread-safety** — [aead] is initialised lazily on first use. After that,
 * Tink primitives are thread-safe and can be called from any coroutine dispatcher.
 */
@Singleton
class SecurityManager @Inject constructor(@ApplicationContext context: Context) {

    // Name of the Tink keyset entry inside [prefName] SharedPreferences file.
    private val keysetName = "master_keyset"

    // SharedPreferences file that stores the encrypted Tink keyset.
    private val prefName = "tink_prefs"

    // URI that identifies the Android Keystore key used to envelope-encrypt
    // the Tink keyset. The Keystore key is created automatically if absent.
    private val masterKeyUri = "android-keystore://_ether_master_key_"

    /**
     * Lazily initialised AEAD (Authenticated Encryption with Associated Data) primitive.
     *
     * Initialisation is deferred to first use because [AndroidKeysetManager.Builder]
     * performs I/O (reading from SharedPreferences and the Android Keystore) and
     * should not block the injection graph construction on the main thread.
     */
    private val aead: Aead by lazy {
        AeadConfig.register()

        AndroidKeysetManager.Builder()
            .withSharedPref(context, keysetName, prefName) // where to persist the keyset
            .withKeyTemplate(KeyTemplates.get("AES256_GCM")) // generate a new key with this template if none exists
            .withMasterKeyUri(masterKeyUri) // Android Keystore key that wraps the stored keyset
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    /**
     * Encrypts [value] with AES-256-GCM and returns the result as a
     * Base64-encoded string safe for storage in DataStore or SharedPreferences.
     *
     * Each call produces a unique ciphertext because Tink generates a fresh
     * random IV (nonce) per encryption — identical plaintexts never produce
     * the same output.
     *
     * @param value The plaintext string to encrypt.
     * @return Base64-encoded ciphertext (includes the GCM authentication tag).
     */
    fun encrypt(value: String): String {
        val ciphertext = aead.encrypt(value.toByteArray(), null)
        // Use android.util.Base64 for standard Android behavior
        return Base64.encodeToString(ciphertext, Base64.DEFAULT)
    }

    /**
     * Decodes [encryptedValue] from Base64, then decrypts and authenticates
     * the ciphertext with AES-256-GCM.
     *
     * Throws if the ciphertext is corrupt, truncated, or was produced by a
     * different keyset — callers (e.g. [EncryptedStorage]) should catch and
     * treat any exception as a missing or unreadable value.
     *
     * @param encryptedValue Base64-encoded ciphertext produced by [encrypt].
     * @return The original plaintext string.
     */
    fun decrypt(encryptedValue: String): String {
        // Use android.util.Base64
        val data = Base64.decode(encryptedValue, Base64.DEFAULT)
        return String(aead.decrypt(data, null))
    }
}