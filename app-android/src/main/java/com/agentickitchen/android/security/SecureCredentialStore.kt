package com.agentickitchen.android.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CredentialStore {
    fun saveCredential(key: String, value: String)
    fun getCredential(key: String): String?
    fun hasCredential(key: String): Boolean
    fun removeCredential(key: String)
    fun clearAll()
}

/**
 * Stores credential ciphertext in private preferences while the AES key remains in Android Keystore.
 * Plaintext credentials are never written to preferences, SQLDelight, logs, or backup payloads.
 */
class SecureCredentialStore(context: Context) : CredentialStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    override fun saveCredential(key: String, value: String) {
        if (value.isBlank()) {
            removeCredential(key)
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = listOf(
            PAYLOAD_VERSION,
            Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        ).joinToString(SEPARATOR)

        check(prefs.edit().putString(key, payload).commit()) {
            "Credential ciphertext could not be persisted"
        }
    }

    override fun getCredential(key: String): String? {
        val payload = prefs.getString(key, null) ?: return null
        return runCatching {
            val parts = payload.split(SEPARATOR, limit = 3)
            require(parts.size == 3 && parts[0] == PAYLOAD_VERSION)
            val iv = Base64.decode(parts[1], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[2], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrNull()?.takeIf(String::isNotBlank)
    }

    override fun hasCredential(key: String): Boolean = getCredential(key) != null

    override fun removeCredential(key: String) {
        check(prefs.edit().remove(key).commit()) {
            "Credential ciphertext could not be removed"
        }
    }

    override fun clearAll() {
        check(prefs.edit().clear().commit()) {
            "Credential ciphertext could not be cleared"
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) return existing

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "agentic_secure_credentials_v2"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "agentic_kitchen_credentials_v2"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PAYLOAD_VERSION = "v1"
        const val SEPARATOR = ":"
    }
}
