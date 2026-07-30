package com.agentickitchen.android.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings
import org.json.JSONArray

class PreferencesManager(context: Context) : AppPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("agentic_prefs", Context.MODE_PRIVATE)

    override fun setupDone() = prefs.getBoolean("setup_done", false)
    override fun saveSetup(done: Boolean, equipment: Set<String>) {
        prefs.edit().putStringSet("equipment", equipment).putBoolean("setup_done", done).apply()
    }
    override fun equipment() = prefs.getStringSet("equipment", setOf("oven", "elec")) ?: setOf("oven", "elec")
    override fun hardwareSettings() = HardwareSettings(
        stoveType = prefs.getString("stove_type", "electric") ?: "electric",
        stovePowerMax = prefs.getInt("stove_power_max", 9),
        ovenAvailable = prefs.getBoolean("oven_available", true),
        ovenHasFan = prefs.getBoolean("oven_has_fan", true),
        ovenHasGrill = prefs.getBoolean("oven_has_grill", false),
        powerLevel = prefs.getInt("power_level", 7),
        geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
        hfApiKey = prefs.getString("hf_api_key", "") ?: "",
        aiProvider = prefs.getString("ai_provider", "FREE") ?: "FREE"
    )
    override fun saveHardwareSettings(settings: HardwareSettings) { prefs.edit().putString("stove_type", settings.stoveType).putInt("stove_power_max", settings.stovePowerMax).putBoolean("oven_available", settings.ovenAvailable).putBoolean("oven_has_fan", settings.ovenHasFan).putBoolean("oven_has_grill", settings.ovenHasGrill).putInt("power_level", settings.powerLevel).putString("gemini_api_key", settings.geminiApiKey).putString("hf_api_key", settings.hfApiKey).putString("ai_provider", settings.aiProvider).apply() }
    override fun dietSettings() = DietSettings(prefs.getString("diet_type", "none") ?: "none", prefs.getStringSet("allergies", emptySet()) ?: emptySet())
    override fun saveDietSettings(settings: DietSettings) { prefs.edit().putString("diet_type", settings.dietType).putStringSet("allergies", settings.allergies).apply() }
    override fun theme() = prefs.getString("theme", "editorial") ?: "editorial"
    override fun saveTheme(theme: String) { prefs.edit().putString("theme", theme).apply() }
    override fun language() = prefs.getString("lang", "Türkçe") ?: "Türkçe"
    override fun saveLanguage(language: String) { prefs.edit().putString("lang", language).apply() }
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
}
