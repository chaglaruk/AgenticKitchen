package com.agentickitchen.android.data.preferences

import com.agentickitchen.android.DietSettings
import com.agentickitchen.android.HardwareSettings

interface AppPreferences {
    fun setupDone(): Boolean
    fun saveSetup(done: Boolean, equipment: Set<String>, servings: Int, mealTime: String)
    fun equipment(): Set<String>
    fun mealTime(): String
    fun hardwareSettings(): HardwareSettings
    fun saveHardwareSettings(settings: HardwareSettings)
    fun dietSettings(): DietSettings
    fun saveDietSettings(settings: DietSettings)
    fun theme(): String
    fun saveTheme(theme: String)
    fun language(): String
    fun saveLanguage(language: String)
}
