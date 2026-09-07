package com.agentickitchen.android.ui

enum class ThemePreference(val storageValue: String) {
    FOLLOW_SYSTEM("FOLLOW_SYSTEM"),
    MODERN_MINIMAL_A("MODERN_MINIMAL_A"),
    PREMIUM_DARK_B("PREMIUM_DARK_B"),
    LUXE_APPLIANCE_DARK_K("LUXE_APPLIANCE_DARK_K"),
    WARM_EDITORIAL_L("WARM_EDITORIAL_L"),
    MINIMAL_PRO_M("MINIMAL_PRO_M");

    companion object {
        fun fromStored(value: String?): ThemePreference = when (value?.trim()?.uppercase()) {
            "MODERN_MINIMAL_A", "EDITORIAL-LIGHT", "EDITORIAL_LIGHT", "LIGHT" -> MODERN_MINIMAL_A
            "PREMIUM_DARK_B", "PREMIUM-DARK", "PREMIUM_DARK" -> PREMIUM_DARK_B
            "LUXE_APPLIANCE_DARK_K", "LUXE-APPLIANCE-DARK", "LUXE_APPLIANCE_DARK", "EDITORIAL-DARK", "EDITORIAL_DARK", "DARK" -> LUXE_APPLIANCE_DARK_K
            "WARM_EDITORIAL_L", "WARM-EDITORIAL", "WARM_EDITORIAL" -> WARM_EDITORIAL_L
            "MINIMAL_PRO_M", "MINIMAL-PRO", "MINIMAL_PRO" -> MINIMAL_PRO_M
            "EDITORIAL", null, "" -> FOLLOW_SYSTEM
            else -> FOLLOW_SYSTEM
        }

        fun resolve(value: String?, systemDark: Boolean): ThemePreference =
            fromStored(value).let { if (it == FOLLOW_SYSTEM) {
                if (systemDark) LUXE_APPLIANCE_DARK_K else MODERN_MINIMAL_A
            } else it }
    }
}
