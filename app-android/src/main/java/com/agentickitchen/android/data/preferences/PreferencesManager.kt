package com.agentickitchen.android.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.agentickitchen.android.AllergyCatalog
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings
import com.agentickitchen.android.security.CredentialMigrator
import com.agentickitchen.android.security.CredentialStore
import com.agentickitchen.android.security.LegacyCredentialSource
import org.json.JSONArray

class PreferencesManager(
    context: Context,
    private val credentialStore: CredentialStore
) : AppPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        CredentialMigrator(
            secureStore = credentialStore,
            legacySource = object : LegacyCredentialSource {
                override fun read(key: String): String? = prefs.getString(key, null)

                override fun remove(keys: Set<String>): Boolean {
                    val editor = prefs.edit()
                    keys.forEach(editor::remove)
                    return editor.commit()
                }
            }
        ).migrate(CREDENTIAL_KEYS)
    }

    override fun setupDone() = prefs.getBoolean("setup_done", false)

    override fun saveSetup(done: Boolean, equipment: Set<String>) {
        prefs.edit().putStringSet("equipment", equipment).putBoolean("setup_done", done).apply()
    }

    override fun equipment() =
        prefs.getStringSet("equipment", setOf("oven", "elec")) ?: setOf("oven", "elec")

    override fun hardwareSettings() = HardwareSettings(
        stoveType = prefs.getString("stove_type", "electric") ?: "electric",
        stovePowerMax = prefs.getInt("stove_power_max", 9),
        ovenAvailable = prefs.getBoolean("oven_available", true),
        ovenHasFan = prefs.getBoolean("oven_has_fan", true),
        ovenHasGrill = prefs.getBoolean("oven_has_grill", false),
        powerLevel = prefs.getInt("power_level", 7),
        geminiApiKey = credentialStore.getCredential(GEMINI_API_KEY).orEmpty(),
        hfApiKey = credentialStore.getCredential(HF_API_KEY).orEmpty(),
        aiProvider = prefs.getString("ai_provider", "FREE") ?: "FREE"
    )

    override fun saveHardwareSettings(settings: HardwareSettings) {
        saveCredential(GEMINI_API_KEY, settings.geminiApiKey)
        saveCredential(HF_API_KEY, settings.hfApiKey)

        check(
            prefs.edit()
                .putString("stove_type", settings.stoveType)
                .putInt("stove_power_max", settings.stovePowerMax)
                .putBoolean("oven_available", settings.ovenAvailable)
                .putBoolean("oven_has_fan", settings.ovenHasFan)
                .putBoolean("oven_has_grill", settings.ovenHasGrill)
                .putInt("power_level", settings.powerLevel)
                .putString("ai_provider", settings.aiProvider)
                .remove(GEMINI_API_KEY)
                .remove(HF_API_KEY)
                .commit()
        ) {
            "Application preferences could not be persisted"
        }
    }

    override fun dietSettings() = DietSettings(
        prefs.getString("diet_type", "none") ?: "none",
        AllergyCatalog.normalize(prefs.getStringSet("allergies", emptySet()) ?: emptySet())
    )

    override fun saveDietSettings(settings: DietSettings) {
        prefs.edit()
            .putString("diet_type", settings.dietType)
            .putStringSet("allergies", AllergyCatalog.normalize(settings.allergies))
            .apply()
    }

    override fun theme() = prefs.getString("theme", "editorial") ?: "editorial"

    override fun saveTheme(theme: String) {
        prefs.edit().putString("theme", theme).apply()
    }

    override fun language() = prefs.getString("lang", "Türkçe") ?: "Türkçe"

    override fun saveLanguage(language: String) {
        prefs.edit().putString("lang", language).apply()
    }

    override fun ingredientDraft(): List<String> {
        val stored = prefs.getString("ingredient_draft", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(stored)
            List(array.length()) { index -> array.getString(index) }
        }.getOrDefault(emptyList())
    }

    override fun saveIngredientDraft(ingredients: List<String>) {
        prefs.edit().putString("ingredient_draft", JSONArray(ingredients).toString()).apply()
    }

    private fun saveCredential(key: String, value: String) {
        if (value.isBlank()) credentialStore.removeCredential(key)
        else credentialStore.saveCredential(key, value)
    }

    private companion object {
        const val PREFS_NAME = "agentic_prefs"
        const val GEMINI_API_KEY = "gemini_api_key"
        const val HF_API_KEY = "hf_api_key"
        val CREDENTIAL_KEYS = setOf(GEMINI_API_KEY, HF_API_KEY)
    }
}
