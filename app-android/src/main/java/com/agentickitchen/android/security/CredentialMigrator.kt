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
                runCatching { secureStore.hasCredential(key) }.getOrDefault(false) -> removable += key
                else -> {
                    val roundTripSucceeded = runCatching {
                        secureStore.saveCredential(key, legacyValue)
                        secureStore.getCredential(key) == legacyValue
                    }.getOrDefault(false)
                    if (roundTripSucceeded) {
                        migrated += key
                        removable += key
                    }
                }
            }
        }

        if (removable.isNotEmpty()) {
            runCatching { legacySource.remove(removable) }
        }
        return migrated
    }
}
