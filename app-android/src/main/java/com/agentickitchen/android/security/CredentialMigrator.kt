package com.agentickitchen.android.security

internal interface LegacyCredentialSource {
    fun read(key: String): String?
    fun remove(keys: Set<String>): Boolean
}

internal class CredentialMigrator(
    private val secureStore: CredentialStore,
    private val legacySource: LegacyCredentialSource
) {
    fun migrate(keys: Set<String>): Set<String> {
        val removable = linkedSetOf<String>()
        val migrated = linkedSetOf<String>()

        keys.forEach { key ->
            val legacyValue = legacySource.read(key) ?: return@forEach
            when {
                legacyValue.isBlank() -> removable += key
                secureStore.hasCredential(key) -> removable += key
                else -> {
                    secureStore.saveCredential(key, legacyValue)
                    if (secureStore.getCredential(key) == legacyValue) {
                        migrated += key
                        removable += key
                    }
                }
            }
        }

        if (removable.isNotEmpty()) {
            check(legacySource.remove(removable)) {
                "Legacy credential plaintext could not be removed"
            }
        }
        return migrated
    }
}
