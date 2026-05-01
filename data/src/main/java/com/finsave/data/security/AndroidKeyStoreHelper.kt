package com.finsave.data.security

import android.content.Context
import android.util.Base64
import com.finsave.core.common.Constants
import com.finsave.core.common.prefs.PreferencesManager
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

object AndroidKeyStoreHelper {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "FinSave_db_encryption_key"
    private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    /**
     * Returns a deterministic 32-byte passphrase derived from a hardware-backed Keystore key.
     *
     * Notes:
     * - The passphrase itself is never stored.
     * - A per-install IV is stored in SharedPreferences; IV is non-secret by design.
     */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = PreferencesManager(context)
        var secretKey = getOrCreateSecretKey()
        val iv = getOrCreateIv(prefs)
        val sentinel = SENTINEL.copyOf()
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            } catch (e: java.security.InvalidAlgorithmParameterException) {
                // The existing key might have been generated with randomized encryption required (the default).
                // Since this app strictly requires a deterministic IV to derive the passphrase,
                // such a key could never have successfully generated a passphrase.
                // We must delete the unusable key and generate a new one.
                val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
                keyStore.deleteEntry(KEY_ALIAS)
                secretKey = getOrCreateSecretKey()
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            }
            val ciphertext = cipher.doFinal(sentinel)
            try {
                val passphrase = ciphertext.copyOfRange(0, 32)
                return passphrase
            } finally {
                Arrays.fill(ciphertext, 0)
            }
        } finally {
            Arrays.fill(sentinel, 0)
            Arrays.fill(iv, 0)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getOrCreateIv(prefs: PreferencesManager): ByteArray {
        val existing = prefs.getString(Constants.PREFS_DB_PASSPHRASE_IV, "")
        if (existing.isNotBlank()) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        prefs.setString(Constants.PREFS_DB_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
        return iv
    }

    private val SENTINEL: ByteArray by lazy {
        val raw = "FinSave_DB_v1_passphrase_salt___".toByteArray(Charsets.UTF_8)
        when {
            raw.size == 32 -> raw
            raw.size > 32 -> raw.copyOfRange(0, 32)
            else -> ByteArray(32).also { out ->
                System.arraycopy(raw, 0, out, 0, raw.size)
            }
        }
    }
}