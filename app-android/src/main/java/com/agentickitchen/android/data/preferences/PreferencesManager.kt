package com.agentickitchen.android.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings

class PreferencesManager(context: Context) : AppPreferences {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("agentic_prefs", Context.MODE_PRIVATE)

    override fun setupDone() = prefs.getBoolean("setup_done", false)
    override fun saveSetup(done: Boolean, equipment: Set<String>, servings: Int, mealTime: String) {
        prefs.edit().putStringSet("equipment", equipment).putInt("setup_servings", servings)
            .putString("meal_time", mealTime).putBoolean("setup_done", done).apply()
    }
    override fun equipment() = prefs.getStringSet("equipment", setOf("oven", "elec")) ?: setOf("oven", "elec")
    override fun mealTime() = prefs.getString("meal_time", "19:00") ?: "19:00"
    override fun hardwareSettings() = HardwareSettings(
        prefs.getString("stove_type", "electric") ?: "electric", prefs.getInt("stove_power_max", 9),
        prefs.getBoolean("oven_available", true), prefs.getBoolean("oven_has_fan", true), prefs.getBoolean("oven_has_grill", false),
        prefs.getInt("serving_size", 2), prefs.getInt("power_level", 7), prefs.getString("gemini_api_key", "") ?: "",
        prefs.getString("hf_api_key", "") ?: "", prefs.getString("ai_provider", "FREE") ?: "FREE"
    )
    override fun saveHardwareSettings(settings: HardwareSettings) { prefs.edit().putString("stove_type", settings.stoveType).putInt("stove_power_max", settings.stovePowerMax).putBoolean("oven_available", settings.ovenAvailable).putBoolean("oven_has_fan", settings.ovenHasFan).putBoolean("oven_has_grill", settings.ovenHasGrill).putInt("serving_size", settings.servingSize).putInt("power_level", settings.powerLevel).putString("gemini_api_key", settings.geminiApiKey).putString("hf_api_key", settings.hfApiKey).putString("ai_provider", settings.aiProvider).apply() }
    override fun dietSettings() = DietSettings(prefs.getString("diet_type", "none") ?: "none", prefs.getStringSet("allergies", emptySet()) ?: emptySet())
    override fun saveDietSettings(settings: DietSettings) { prefs.edit().putString("diet_type", settings.dietType).putStringSet("allergies", settings.allergies).apply() }
    override fun theme() = prefs.getString("theme", "editorial") ?: "editorial"
    override fun saveTheme(theme: String) { prefs.edit().putString("theme", theme).apply() }
    override fun language() = prefs.getString("lang", "Türkçe") ?: "Türkçe"
    override fun saveLanguage(language: String) { prefs.edit().putString("lang", language).apply() }
}
